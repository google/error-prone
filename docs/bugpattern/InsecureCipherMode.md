This error is triggered when a Cipher instance is created using either the
default settings or the notoriously insecure ECB mode. In particular, Java's
default `Cipher.getInstance(AES)` returns a cipher object that operates in ECB
mode. Dynamically constructed transformation strings are also flagged, as they
may conceal an instance of ECB mode. The problem with ECB mode is that
encrypting the same block of plaintext always yields the same block of
ciphertext. Hence, repetitions in the plaintext translate into repetitions in
the ciphertext, which can be readily used to conduct cryptanalysis. The use of
IES-based cipher algorithms also raises an error, as all currently available
implementations use ECB mode under the hood.

