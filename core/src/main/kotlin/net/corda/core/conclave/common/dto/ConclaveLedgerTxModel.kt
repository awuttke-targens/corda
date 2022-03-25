package net.corda.core.conclave.common.dto

import net.corda.core.contracts.Attachment
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.node.NotaryInfo
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction
import java.time.Instant

@CordaSerializable
data class ConclaveNetworkParams(
        val minimumPlatformVersion: Int,
        val notaries: List<NotaryInfo>,
        val maxMessageSize: Int,
        val maxTransactionSize: Int,
        val modifiedTime: Instant,
        val epoch: Int
)

/**
 * Enclave representation of a signed and ledger transaction.
 * ConclaveLedgerTxModel wraps a [SignedTransaction] and additional properties to allow an enclave to reconstruct and
 * verify a ledger transaction.
 * @property signedTransaction a serializable transaction with signatures.
 * @property inputStates an array of input states that will be consumed by the wrapped transaction.
 * @property attachments an array of attachment objects that are required for this transaction to verify.
 * @property networkParameters the network parameters that were in force when the enclosed wire transaction was
 *  constructed.
 * @property references an array of reference states.
 */
@CordaSerializable
data class ConclaveLedgerTxModel(
        val signedTransaction: SignedTransaction,
        val inputStates: Array<StateAndRef<ContractState>>,
        val attachments: Array<Attachment>,
        val networkParameters: ConclaveNetworkParams,
        val references: Array<StateAndRef<ContractState>>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConclaveLedgerTxModel

        if (signedTransaction != other.signedTransaction) return false
        if (!inputStates.contentEquals(other.inputStates)) return false
        if (!attachments.contentEquals(other.attachments)) return false
        if (networkParameters != other.networkParameters) return false
        if (!references.contentEquals(other.references)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = signedTransaction.hashCode()
        result = 31 * result + inputStates.contentHashCode()
        result = 31 * result + attachments.contentHashCode()
        result = 31 * result + networkParameters.hashCode()
        result = 31 * result + references.contentHashCode()
        return result
    }
}