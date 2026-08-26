# Ruby Stack

Use this stack when the target project needs Ruby, Rails, Rack, or Ruby-first
service tooling.

## Contains

- `Gemfile.example`: minimal Bundler policy with a public RubyGems cooldown.
- `scripts/{setup,dev,lint,test,check-all}.sh`: validation and local
  development wrappers for a copied Ruby stack.

The root devkit keeps `scripts/worktree-ports.sh` as the canonical port helper
so projects are not forced to include Ruby just for port allocation.
If an ADE or workspace orchestrator has its own reserved port variable, map it
to `PORT` or `WORKTREE_PRIMARY_PORT` in that environment's wrapper script
instead of adding ADE-specific names to this stack.

## Apply

Copy the relevant files into the target repo root:

```bash
mkdir -p scripts
cp stacks/ruby/Gemfile.example Gemfile
cp scripts/worktree-ports.sh scripts/
cp stacks/ruby/scripts/*.sh scripts/
```

Then check the local Bundler version:

```bash
bundle --version
```

Bundler cooldowns require Bundler `4.0.13` or newer. If the target repo has an
older Bundler, ask before upgrading. After upgrading, pin Bundler in the
lockfile:

```bash
gem install bundler -v 4.0.13
bundle install
bundle lock --bundler=4.0.13
```

Then validate the copied stack:

```bash
./scripts/check-all.sh
```

## Agent Notes

- Do not copy this stack just because a repo has scripts. Select it only when
  Ruby is part of the target runtime or tooling.
- Keep `source "https://rubygems.org", cooldown: 7` on public RubyGems sources
  when Bundler is `4.0.13` or newer.
- Use `cooldown: 0`, `bundle install --cooldown 0`, or `BUNDLE_COOLDOWN=0`
  only for intentional exceptions such as urgent security fixes.
- Commit `Gemfile.lock` after applying the stack to a real target repo.
- Keep root port helpers shell-based unless the target repo intentionally wants
  Ruby helper scripts.
- If the target repo already has its own port helper, adapt `scripts/dev.sh`
  instead of overwriting that helper.
- If the target ADE exposes a product-specific port variable, map it to `PORT`
  or `WORKTREE_PRIMARY_PORT` outside the reusable stack scripts.
- Update `.env.example` whenever settings fields change.
- Run `./scripts/check-all.sh` from the copied Ruby stack before handing off.
