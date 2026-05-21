#!/usr/bin/env bash
# Updates zio-ulid repository description, homepage, and topics on GitHub.
# Requires: curl, GITHUB_TOKEN (classic or fine-grained with repo metadata write)

set -euo pipefail

OWNER="Er-dhirendra"
REPO="zio-ulid"
TOKEN="${GITHUB_TOKEN:-}"

if [[ -z "$TOKEN" ]]; then
  echo "Set GITHUB_TOKEN to update repository metadata on GitHub."
  exit 1
fi

DESCRIPTION="Type-safe, purely functional ULID generation for ZIO 2.x — sortable IDs with live, monotonic, and deterministic layers."
HOMEPAGE="https://central.sonatype.com/artifact/dev.zio/zio-ulid_3"

curl -fsS -X PATCH \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Accept: application/vnd.github+json" \
  "https://api.github.com/repos/${OWNER}/${REPO}" \
  -d "$(jq -n --arg d "$DESCRIPTION" --arg h "$HOMEPAGE" '{description:$d, homepage:$h}')"

TOPICS='["zio","ulid","scala","scala3","functional-programming","library","maven-central","identifiers"]'

curl -fsS -X PUT \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Accept: application/vnd.github.mercy-preview+json" \
  "https://api.github.com/repos/${OWNER}/${REPO}/topics" \
  -d "$(jq -n --argjson t "$TOPICS" '{names: $t}')"

echo "Updated ${OWNER}/${REPO} description, homepage, and topics."
