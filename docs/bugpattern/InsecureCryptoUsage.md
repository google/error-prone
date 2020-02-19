This checker looks for usages of standard cryptographic algorithms in
configurations that are prone to vulnerabilities. There are currently a few
classes of problems that are covered by this checker:

*   Creating an instance of any of the following is considered insecure:

    *   `javax.crypto.Cipher`
    *   `javax.crypto.KeyAgreement`
    *   `javax.crypto.Mac`
    *   `javax.crypto.CipherInputStream`
    *   `javax.crypto.CipherOutputStream`
    *   `java.security.KeyPairGenerator`
    *   `java.security.MessageDigest`
    *   `java.security.Signature`

