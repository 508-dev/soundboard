#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")/.."

if ! command -v bundle >/dev/null 2>&1; then
  echo "Bundler is required. Install Bundler 4.0.13 or newer before setup." >&2
  exit 1
fi

version="$(bundle --version | awk '{print $3}')"
if ! ruby -e 'exit(Gem::Version.new(ARGV[0]) >= Gem::Version.new("4.0.13") ? 0 : 1)' "$version"; then
  echo "Bundler ${version} is too old for Gemfile cooldown syntax; install Bundler 4.0.13 or newer." >&2
  exit 1
fi

bundle install
