# Add project-specific ProGuard/R8 rules here as release-build issues surface.
# See https://developer.android.com/build/shrink-code for the general shape.

# kotlinx.serialization keeps its own consumer rules via the library's
# embedded proguard config, so no manual @Serializable rules are needed here
# unless R8 output testing turns up a gap.
