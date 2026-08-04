# OpenSSH Ed25519 test fixtures

These unencrypted key pairs were generated only for cross-platform parser tests:

```sh
ssh-keygen -q -t ed25519 -N "" -C "java-guard matching test fixture" -f matching
ssh-keygen -q -t ed25519 -N "" -C "java-guard mismatch test fixture" -f other
```

The private keys are public test material. They must never be used for production signing.

Expected raw Ed25519 public keys:

```text
matching: 430c96393033fcf523590a21d9a663c30fc139fe1d8596b0098620cc6ed8fcfe
other:    aa2b3c1fe3a62eac10a8558d34b8b823692211646863730687fa377b227204f5
```

Expected SHA-256 hashes:

```text
29678d855f6fe6d53fce3c9af9691d9b400b6cb5ab225df3c3ea89d285114e63  matching
8cb8b4fa5c124c8a5faa6251ae55be0ac3dbcc423d34d8c222a734ba1ee7fa39  matching.pub
d731ec32a0fbdda04f42e0e2ae97858e3ce16f0beb7932787e2f31af683349bf  other
ef7d90ca18f3654153a43a06bcfa5c6d6d6ce8ef1f6f49eb836731db08617a8e  other.pub
```
