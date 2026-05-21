# GitHub profile checklist

Manual updates on https://github.com/settings/profile (API cannot set all fields without a token).

## Account bio (suggested)

```
Lead Software Engineer @ Avlino | Scala · ZIO · Kafka · Spark | Open source: zio-ulid
```

## Pinned repositories (suggested order)

1. `zio-ulid`
2. `attendance-system`
3. `scala-design-pattern`
4. `scala-SOLID-principle`

## Repository metadata (`zio-ulid`)

Run locally after creating a GitHub token with `repo` scope:

```bash
export GITHUB_TOKEN=ghp_...
chmod +x scripts/update-github-repo.sh
./scripts/update-github-repo.sh
```

Or set manually under **Repository → Settings → General**:

- **Description:** Type-safe, purely functional ULID generation for ZIO 2.x — sortable IDs with live, monotonic, and deterministic layers.
- **Website:** https://central.sonatype.com/artifact/dev.zio/zio-ulid_3
- **Topics:** `zio`, `ulid`, `scala`, `scala3`, `functional-programming`, `library`, `maven-central`, `identifiers`
