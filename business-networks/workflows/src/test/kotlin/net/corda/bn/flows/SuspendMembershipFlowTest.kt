package net.corda.bn.flows

import net.corda.bn.contracts.MembershipContract
import net.corda.bn.states.BNORole
import net.corda.bn.states.MembershipState
import net.corda.bn.states.MembershipStatus
import net.corda.core.contracts.UniqueIdentifier
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SuspendMembershipFlowTest : MembershipManagementFlowTest(numberOfAuthorisedMembers = 1, numberOfRegularMembers = 2) {

    @Test(timeout = 300_000)
    fun `suspend membership flow should fail if membership with given ID doesn't exist`() {
        val authorisedMember = authorisedMembers.first()

        val invalidMembershipId = UniqueIdentifier()
        assertFailsWith<MembershipNotFoundException> { runSuspendMembershipFlow(authorisedMember, invalidMembershipId) }
    }

    @Test(timeout = 300_000)
    fun `suspend membership flow should fail if initiator is not part of the business network or if its membership is not active`() {
        val authorisedMember = authorisedMembers.first()
        val regularMember = regularMembers.first()
        val nonMember = regularMembers[1]

        val networkId = (runCreateBusinessNetworkFlow(authorisedMember).tx.outputStates.single() as MembershipState).networkId
        val membership = runRequestMembershipFlow(regularMember, authorisedMember, networkId).tx.outputStates.single() as MembershipState

        assertFailsWith<MembershipNotFoundException> { runSuspendMembershipFlow(nonMember, membership.linearId) }

        runRequestAndSuspendMembershipFlow(nonMember, authorisedMember, networkId).apply {
            val membership = tx.outputStates.single() as MembershipState
            runModifyRolesFlow(authorisedMember, membership.linearId, setOf(BNORole()))
        }
        assertFailsWith<IllegalMembershipStatusException> { runSuspendMembershipFlow(nonMember, membership.linearId) }
    }

    @Test(timeout = 300_000)
    fun `suspend membership flow should fail if invalid notary argument is provided`() {
        val authorisedMember = authorisedMembers.first()
        val regularMember = regularMembers.first()

        val networkId = (runCreateBusinessNetworkFlow(authorisedMember).tx.outputStates.single() as MembershipState).networkId

        val membership = runRequestMembershipFlow(regularMember, authorisedMember, networkId).tx.outputStates.single() as MembershipState
        assertFailsWith<IllegalArgumentException> { runSuspendMembershipFlow(authorisedMember, membership.linearId, authorisedMember.identity()) }
    }

    @Test(timeout = 300_000)
    fun `suspend membership flow happy path`() {
        val authorisedMember = authorisedMembers.first()
        val regularMember = regularMembers.first()

        val networkId = (runCreateBusinessNetworkFlow(authorisedMember).tx.outputStates.single() as MembershipState).networkId

        val (membership, command) = runRequestAndSuspendMembershipFlow(regularMember, authorisedMember, networkId).run {
            assertEquals(1, tx.inputs.size)
            verifyRequiredSignatures()
            tx.outputs.single() to tx.commands.single()
        }

        membership.apply {
            assertEquals(MembershipContract.CONTRACT_NAME, contract)
            assertTrue(data is MembershipState)
            val data = data as MembershipState
            assertEquals(regularMember.identity(), data.identity)
            assertEquals(networkId, data.networkId)
            assertEquals(MembershipStatus.SUSPENDED, data.status)
        }
        assertTrue(command.value is MembershipContract.Commands.Suspend)

        // also check ledgers
        listOf(authorisedMember, regularMember).forEach { member ->
            getAllMembershipsFromVault(member, networkId).apply {
                assertEquals(2, size, "Vault size assertion failed for ${member.identity()}")
                assertTrue(any { it.identity == authorisedMember.identity() }, "Expected to have ${authorisedMember.identity()} in ${member.identity()} vault")
                assertTrue(any { it.identity == regularMember.identity() }, "Expected to have ${regularMember.identity()} in ${member.identity()} vault")
            }
        }
    }
}