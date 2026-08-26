This is an android app. The purpose of the app is to allow users to choose custom sound files to display in the app's ui. They can then play those sound files independently of eachoother (play one or multiple at the same time; stop one while others continue to play). They can also independently control the volume of each sound file.

The app's main screen is a layout of the sound files, displayed as full-width rounded rectangles. The right side of each rectangle has a play/pause toggle button. There is a percentage number indicating the current volume (100% is the max volume per the android system's current volume, less than 100% reduces the sound's volume relative to the main volume.), which when tapped, opens a volume selector circle (tap and slide to change percent relative volume). Relative volumes maintain their relation to main android system volume even when it changes.

There is a "+" FAB that when tapped, open's the android sound selector system. When a sound is selected, it's imported into the app. Ideally, the app uses the relative location of the file sound, rather than copying it into internal memory.

Each rectangle can be long-pressed to launch a modal option selector. For now the only selectable button is "delete," which will trigger a confirmation window that indicates that the sound will be removed from the app's list, but the file is not deleted. When confirmed, the sound is removed from the app's list.

This repo contains a template for 508.dev, an engineering co-op. It's tailored more for webdev type projects, and doesn't have tooling or anything for android stuff, as far as I know. Part of our goal is to delete anything we don't need, or adjust as needed for android workflows.

This environment has android studio installed. Any further tooling needed to be installed may be requested.

Any uncertainties should be clarified before making a decision.
