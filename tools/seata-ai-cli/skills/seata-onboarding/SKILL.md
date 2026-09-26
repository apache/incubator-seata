---
name: seata-onboarding
description: Set up a Seata AI CLI Profile and browser authentication with explicit endpoint and trust choices.
---

# seata-onboarding

Start with setup.inspect, which is local and network-free by default. Show candidate endpoints, environment scope and trust policy; obtain the user's target decision before setup.initialize. Inspect command help/schema rather than guessing missing endpoints or flags.

Check auth.status with verification for the selected Profile. When login is necessary, use auth.login and the local browser flow, then auth.login.status. Never ask the user to paste a password or token into the conversation. If the URL cannot open, check that browser and helper share loopback and that the helper remains alive. Do not create public listeners or tunnels.

Run environment connectivity check and inspect namespace, cluster and server discovery. A working route is not version certification or proof of every management capability. Do not infer a different Profile or accept changed trust on the user's behalf.

For evidence-reporting limits, read [references/evidence.md](references/evidence.md). A worked decision is in [examples/scenario.md](examples/scenario.md); the behavioral acceptance case is in [tests/acceptance.md](tests/acceptance.md).
