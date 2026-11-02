package com.nova.a12tester.data

import com.nova.a12tester.domain.*
import kotlinx.coroutines.flow.update

/**
 * Cohort Team Engine (Team 1, Team 2, Team 3...).
 *
 * Rules & Process:
 * 1. User pays 50 coins to join an active cohort team (Team 1).
 * 2. Each team accommodates up to 13 developers with their submitted apps.
 * 3. When a team reaches 13 members:
 *    - The team locks into ACTIVE 14-day testing mode.
 *    - Team 2 (or next number) is automatically created for incoming developers.
 *    - All 13 developers are assigned the remaining 12 apps of their peers.
 *    - Each user must test each of the 12 apps daily for 2 minutes for 14 days.
 */
fun TestPilotRepository.joinCohortTeam(appId: String, targetTeamId: String? = null): Boolean {
    val current = _state.value.currentUser ?: return false
    val app = _state.value.apps.firstOrNull { it.appId == appId && it.developerId == current.userId }
    if (app == null) {
        _state.update { it.copy(errorMessage = "Please select a valid submitted app of yours to enter the team.") }
        return false
    }

    val existingTeam = _state.value.cohortTeams.firstOrNull { team ->
        team.members.any { it.userId == current.userId }
    }
    if (existingTeam != null) {
        _state.update {
            it.copy(errorMessage = "You are already a member of ${existingTeam.teamName} with app '${existingTeam.members.first { m -> m.userId == current.userId }.appName}'.")
        }
        return false
    }

    val targetTeam = (if (targetTeamId != null) _state.value.cohortTeams.firstOrNull { it.teamId == targetTeamId } else null)
        ?: getActiveOrRecruitingTeam()

    if (targetTeam.isFull) {
        _state.update {
            it.copy(errorMessage = "${targetTeam.teamName} is already full (13/13). Please join an open recruiting team.")
        }
        return false
    }

    val joinCost = _state.value.rewardConfig.teamJoinCostCoins // 200 coins
    if (current.coinBalance < joinCost) {
        _state.update {
            it.copy(
                errorMessage = "$joinCost coins required to join ${targetTeam.teamName}. Your balance: ${current.coinBalance}. Watch rewarded ads or complete tests to earn coins!"
            )
        }
        return false
    }

    // Debit coins using idempotent transaction
    val debited = debitUserInternal(
        userId = current.userId,
        type = TransactionType.TEAM_JOIN_FEE,
        amount = joinCost,
        transactionId = "join_team_${targetTeam.teamId}_${current.userId}",
        referenceId = targetTeam.teamId,
        description = "Joined ${targetTeam.teamName} closed testing cohort"
    )
    if (!debited) {
        _state.update { it.copy(errorMessage = "Failed to process $joinCost coin transaction. Please try again.") }
        return false
    }

    val newMember = TeamMember(
        userId = current.userId,
        userDisplayName = current.displayName,
        userEmail = current.email,
        userPhotoUrl = current.photoUrl,
        appId = app.appId,
        appName = app.appName,
        packageName = app.packageName,
        appIconUrl = app.iconUrl,
        optInUrl = app.optInUrl,
        googleGroupUrl = app.googleGroupUrl,
        joinedAtEpochMs = System.currentTimeMillis()
    )

    val updatedMembers = targetTeam.members + newMember
    val now = System.currentTimeMillis()

    var nextTeamToCreate: TestingCohortTeam? = null

    if (updatedMembers.size >= targetTeam.maxMembers) {
        // Team is FULL (13 members)!
        val activatedTeam = targetTeam.copy(
            members = updatedMembers,
            memberIds = updatedMembers.map { it.userId },
            status = CohortTeamStatus.ACTIVE,
            startDateEpochMs = now,
            endsAtEpochMs = now + 14L * 86_400_000L
        )

        // Automatically create next team (e.g. Team 2 when Team 1 fills)
        val nextTeamNumber = targetTeam.teamNumber + 1
        val alreadyExists = _state.value.cohortTeams.any { it.teamNumber == nextTeamNumber }
        if (!alreadyExists) {
            nextTeamToCreate = TestingCohortTeam(
                teamId = "team_$nextTeamNumber",
                teamNumber = nextTeamNumber,
                teamName = "Team $nextTeamNumber",
                maxMembers = 13,
                members = emptyList(),
                memberIds = emptyList(),
                status = CohortTeamStatus.RECRUITING,
                joinCostCoins = joinCost,
                testingDurationDays = 14,
                dailyMinutesRequired = 2
            )
        }

        // Generate mutual testing assignments: each member gets the other 12 apps!
        val newAssignments = mutableListOf<TestingAssignment>()
        for (member in updatedMembers) {
            val peerMembers = updatedMembers.filter { it.userId != member.userId }
            for (peer in peerMembers) {
                val assignmentId = "${peer.appId}_${member.userId}"
                if (_state.value.assignments.none { it.assignmentId == assignmentId }) {
                    newAssignments.add(
                        TestingAssignment(
                            assignmentId = assignmentId,
                            groupId = activatedTeam.teamId,
                            appId = peer.appId,
                            appName = peer.appName,
                            packageName = peer.packageName,
                            appIconUrl = peer.appIconUrl,
                            developerId = peer.userId,
                            developerName = peer.userDisplayName,
                            testerId = member.userId,
                            testerDisplayName = member.userDisplayName,
                            optInUrl = peer.optInUrl,
                            googleGroupUrl = peer.googleGroupUrl,
                            testingInstructions = "${activatedTeam.teamName} reciprocal test: Open and test for at least 2 minutes daily for 14 days.",
                            requiredDays = 14,
                            reportedDaysCount = 0,
                            verifiedCheckInsCount = 0,
                            groupJoinedConfirmed = true,
                            optInConfirmed = true,
                            installConfirmed = true,
                            status = AssignmentStatus.IN_PROGRESS,
                            rewardCoins = _state.value.apps.firstOrNull { it.appId == peer.appId }?.rewardPerTesterCoins ?: 25,
                            assignedAtEpochMs = now
                        )
                    )
                }
            }
        }

        val joinNotif = AppNotification(
            notificationId = "notif_team_activated_${activatedTeam.teamId}",
            userId = current.userId,
            title = "${activatedTeam.teamName} is Full & Active! 🎉",
            body = "13 developers joined. You are now assigned the other 12 apps to test daily for 2 minutes for 14 days.",
            type = NotificationType.ASSIGNMENT,
            deepLinkRoute = "active_assignments"
        )

        _state.update { st ->
            val baseTeams = st.cohortTeams.map { if (it.teamId == targetTeam.teamId) activatedTeam else it }
            val updatedTeams = if (nextTeamToCreate != null && baseTeams.none { it.teamId == nextTeamToCreate.teamId }) {
                baseTeams + nextTeamToCreate
            } else {
                baseTeams
            }
            st.copy(
                cohortTeams = updatedTeams,
                assignments = st.assignments + newAssignments,
                notifications = st.notifications + joinNotif,
                successMessage = "${activatedTeam.teamName} reached 13 developers and locked to ACTIVE! 12 peer apps assigned." +
                        if (nextTeamToCreate != null) " ${nextTeamToCreate.teamName} automatically created!" else ""
            )
        }
    } else {
        // Team still recruiting (e.g. 1..12 of 13)
        val updatedTeam = targetTeam.copy(members = updatedMembers, memberIds = updatedMembers.map { it.userId })
        val remaining = updatedTeam.remainingSlots

        val peerAssignments = updatedMembers
            .filter { it.userId != current.userId }
            .map { peer ->
                TestingAssignment(
                    assignmentId = "${peer.appId}_${current.userId}",
                    groupId = updatedTeam.teamId,
                    appId = peer.appId,
                    appName = peer.appName,
                    packageName = peer.packageName,
                    appIconUrl = peer.appIconUrl,
                    developerId = peer.userId,
                    developerName = peer.userDisplayName,
                    testerId = current.userId,
                    testerDisplayName = current.displayName,
                    optInUrl = peer.optInUrl,
                    googleGroupUrl = peer.googleGroupUrl,
                    testingInstructions = "Test for at least 2 minutes daily for 14 days.",
                    requiredDays = 14,
                    reportedDaysCount = 0,
                    verifiedCheckInsCount = 0,
                    groupJoinedConfirmed = true,
                    optInConfirmed = true,
                    installConfirmed = true,
                    status = AssignmentStatus.IN_PROGRESS,
                    rewardCoins = _state.value.apps.firstOrNull { it.appId == peer.appId }?.rewardPerTesterCoins ?: 25,
                    assignedAtEpochMs = now
                )
            }

        val joinNotif = AppNotification(
            notificationId = "notif_team_joined_${updatedTeam.teamId}_${current.userId}",
            userId = current.userId,
            title = "Joined ${updatedTeam.teamName}! 🚀",
            body = "You joined slot #${updatedMembers.size} of 13. Only $remaining slot(s) remaining until 14-day cycle locks.",
            type = NotificationType.ASSIGNMENT,
            deepLinkRoute = "home"
        )

        _state.update { st ->
            st.copy(
                cohortTeams = st.cohortTeams.map { if (it.teamId == targetTeam.teamId) updatedTeam else it },
                assignments = st.assignments + peerAssignments.filter { p -> st.assignments.none { it.assignmentId == p.assignmentId } },
                notifications = st.notifications + joinNotif,
                successMessage = "Successfully joined ${updatedTeam.teamName} (Slot ${updatedMembers.size}/13). $joinCost coins debited."
            )
        }
    }

    FirestoreRepository.syncToFirestore {
        _state.value.cohortTeams.firstOrNull { it.teamId == targetTeam.teamId }?.let {
            FirestoreRepository.saveTeam(it)
        }
        if (nextTeamToCreate != null) {
            FirestoreRepository.saveTeam(nextTeamToCreate)
        }
        val currentUserId = current.userId
        _state.value.assignments.filter { it.testerId == currentUserId }.forEach { a ->
            FirestoreRepository.saveAssignment(a)
        }
    }
    return true
}

/**
 * Gets the current recruiting or active cohort team, automatically ensuring
 * a new recruiting team exists if all existing teams are full.
 */
fun TestPilotRepository.getActiveOrRecruitingTeam(): TestingCohortTeam {
    val existing = _state.value.cohortTeams.firstOrNull { it.status == CohortTeamStatus.RECRUITING && !it.isFull }
    if (existing != null) return existing

    val maxNumber = _state.value.cohortTeams.maxOfOrNull { it.teamNumber } ?: 0
    if (maxNumber > 0) {
        val nextNumber = maxNumber + 1
        val newTeam = TestingCohortTeam(
            teamId = "team_$nextNumber",
            teamNumber = nextNumber,
            teamName = "Team $nextNumber",
            maxMembers = 13,
            members = emptyList(),
            memberIds = emptyList(),
            status = CohortTeamStatus.RECRUITING
        )
        _state.update { it.copy(cohortTeams = it.cohortTeams + newTeam) }
        FirestoreRepository.syncToFirestore {
            FirestoreRepository.saveTeam(newTeam)
        }
        return newTeam
    }

    val team1 = TestingCohortTeam(
        teamId = "team_1",
        teamNumber = 1,
        teamName = "Team 1",
        maxMembers = 13,
        members = emptyList(),
        memberIds = emptyList(),
        status = CohortTeamStatus.RECRUITING
    )
    _state.update { it.copy(cohortTeams = listOf(team1)) }
    FirestoreRepository.syncToFirestore {
        FirestoreRepository.saveTeam(team1)
    }
    return team1
}

/**
 * Ensures that whenever a team is full (13 members), the next team (Team 2, Team 3, etc.)
 * is automatically created and ready for incoming developers.
 */
fun TestPilotRepository.ensureRecruitingTeamAvailable(
    currentTeams: List<TestingCohortTeam>
): List<TestingCohortTeam> {
    if (currentTeams.isEmpty()) {
        val team1 = getActiveOrRecruitingTeam()
        return listOf(team1)
    }

    var list = currentTeams
    var needsStateUpdate = false
    val newlyCreatedTeams = mutableListOf<TestingCohortTeam>()

    // Check each team in order: if a team is full (13/13 members), ensure the next team exists!
    val sorted = list.sortedBy { it.teamNumber }
    for (team in sorted) {
        if (team.isFull) {
            val nextNumber = team.teamNumber + 1
            if (list.none { it.teamNumber == nextNumber }) {
                val nextTeam = TestingCohortTeam(
                    teamId = "team_$nextNumber",
                    teamNumber = nextNumber,
                    teamName = "Team $nextNumber",
                    maxMembers = 13,
                    members = emptyList(),
                    memberIds = emptyList(),
                    status = CohortTeamStatus.RECRUITING,
                    joinCostCoins = 200,
                    testingDurationDays = 14,
                    dailyMinutesRequired = 2
                )
                list = list + nextTeam
                newlyCreatedTeams.add(nextTeam)
                needsStateUpdate = true
            }
        }
    }

    // Also, if ALL teams in the list are full, ensure at least one recruiting team is available
    if (list.all { it.isFull }) {
        val maxNumber = list.maxOfOrNull { it.teamNumber } ?: list.size
        val nextNumber = maxNumber + 1
        val nextTeam = TestingCohortTeam(
            teamId = "team_$nextNumber",
            teamNumber = nextNumber,
            teamName = "Team $nextNumber",
            maxMembers = 13,
            members = emptyList(),
            memberIds = emptyList(),
            status = CohortTeamStatus.RECRUITING,
            joinCostCoins = 200,
            testingDurationDays = 14,
            dailyMinutesRequired = 2
        )
        list = list + nextTeam
        newlyCreatedTeams.add(nextTeam)
        needsStateUpdate = true
    }

    if (needsStateUpdate) {
        _state.update { it.copy(cohortTeams = list) }
        FirestoreRepository.syncToFirestore {
            newlyCreatedTeams.forEach { team ->
                FirestoreRepository.saveTeam(team)
            }
        }
    }

    return list
}

/**
 * Manually creates the next cohort team (Team 2, Team 3, ...).
 */
fun TestPilotRepository.createNewCohortTeam(): TestingCohortTeam {
    val existingTeams = _state.value.cohortTeams
    val maxNumber = existingTeams.maxOfOrNull { it.teamNumber } ?: existingTeams.size
    val nextNumber = if (maxNumber <= 0) 1 else maxNumber + 1
    val newTeam = TestingCohortTeam(
        teamId = "team_$nextNumber",
        teamNumber = nextNumber,
        teamName = "Team $nextNumber",
        maxMembers = 13,
        members = emptyList(),
        memberIds = emptyList(),
        status = CohortTeamStatus.RECRUITING
    )
    _state.update {
        it.copy(
            cohortTeams = it.cohortTeams + newTeam,
            successMessage = "${newTeam.teamName} created successfully!"
        )
    }
    FirestoreRepository.syncToFirestore {
        FirestoreRepository.saveTeam(newTeam)
    }
    return newTeam
}

/**
 * Ensures a peer assignment exists for a cohort team member so usage and progress can be tracked reliably.
 */
fun TestPilotRepository.ensurePeerAssignment(peer: TeamMember, currentUserId: String, teamId: String): TestingAssignment {
    val aId = "${peer.appId}_${currentUserId}"
    val existing = _state.value.assignments.firstOrNull { it.assignmentId == aId }
    if (existing != null) return existing

    val current = _state.value.currentUser
    val assignment = TestingAssignment(
        assignmentId = aId,
        groupId = teamId,
        appId = peer.appId,
        appName = peer.appName,
        packageName = peer.packageName,
        appIconUrl = peer.appIconUrl,
        developerId = peer.userId,
        developerName = peer.userDisplayName,
        testerId = currentUserId,
        testerDisplayName = current?.displayName ?: "Tester",
        optInUrl = peer.optInUrl,
        googleGroupUrl = peer.googleGroupUrl,
        testingInstructions = "Team reciprocal test: Open and test for at least 2 minutes daily for 14 days.",
        requiredDays = 14,
        reportedDaysCount = 0,
        verifiedCheckInsCount = 0,
        groupJoinedConfirmed = true,
        optInConfirmed = true,
        installConfirmed = true,
        status = AssignmentStatus.IN_PROGRESS,
        rewardCoins = _state.value.apps.firstOrNull { it.appId == peer.appId }?.rewardPerTesterCoins ?: 25,
        assignedAtEpochMs = System.currentTimeMillis()
    )
    _state.update { st ->
        st.copy(assignments = st.assignments + assignment)
    }
    FirestoreRepository.syncToFirestore {
        FirestoreRepository.saveAssignment(assignment)
    }
    return assignment
}

