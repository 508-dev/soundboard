#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")/.."

bundle exec rubocop
