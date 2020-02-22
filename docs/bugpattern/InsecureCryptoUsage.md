This checker looks for usages of standard cryptographic algorithms in
configurations that are prone to vulnerabilities. There are currently three
classes of problems that are covered by this checker:

*   Creating an instance of `javax.crypto.Cipher` using either the default
    settings or the notoriously insecure ECB mode. In particular, Java's default
    `Cipher.getInstance(AES)` returns a cipher object that operates in ECB mode.
    Dynamically constructed transformation strings are also flagged, as they may
    conceal an instance of ECB mode. The problem with ECB mode is that
    encrypting the same block of plaintext always yields the same block of
    ciphertext. Hence, repetitions in the plaintext translate into repetitions
    in the ciphertext, which can be readily used to conduct cryptanalysis. The
    use of IES-based cipher algorithms also raises an error, as all currently
    available implementations use ECB mode under the hood.

*   Using the Diffie-Hellmann protocol on prime fields. Most library
    implementations of Diffie-Hellman on prime fields have serious issues that
    can be exploited by an attacker. Any operation that may involve this
    protocol will be flagged by the checker. Implementations of the protocol
    based on elliptic curves (ECDH) are secure and should be used instead.

*   Using DSA for digital signatures. Some widely used crypto libraries accept
    invalid DSA signatures in specific configurations. The checker will flag all
    cryptographic operations that may involve DSA.

