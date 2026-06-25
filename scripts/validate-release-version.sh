#!/usr/bin/env bash
set -euo pipefail

version=$(sed -n "s/^def projectVersion = '\([^']*\)'/\1/p" build.gradle | head -n 1)
if [ -z "$version" ]; then
  echo "Unable to read projectVersion from build.gradle"
  exit 1
fi

case "${BASE_REF:?BASE_REF is required}" in
  1.x)
    major=1
    ;;
  2.x)
    major=2
    ;;
  *)
    echo "Unsupported release branch: $BASE_REF"
    exit 1
    ;;
esac

if [[ ! "$version" =~ ^${major}\.[0-9]+\.[0-9]+$ ]]; then
  echo "projectVersion must be a ${major}.x.x release version without -SNAPSHOT: $version"
  exit 1
fi

tag="v${version}"
if git rev-parse -q --verify "refs/tags/${tag}" >/dev/null; then
  echo "Release tag already exists: $tag"
  exit 1
fi

if [ "${REQUIRE_VERSION_BUMP:-false}" = "true" ]; then
  base_version=$(git show "origin/${BASE_REF}:build.gradle" |
    sed -n "s/^def projectVersion = '\([^']*\)'/\1/p" |
    head -n 1)

  if [ "$version" = "$base_version" ]; then
    echo "projectVersion must be bumped from ${base_version} before merging"
    exit 1
  fi
fi

if [ -n "${GITHUB_OUTPUT:-}" ]; then
  echo "version=$version" >> "$GITHUB_OUTPUT"
  echo "tag=$tag" >> "$GITHUB_OUTPUT"
fi

echo "Validated release version ${version} for ${BASE_REF}"
