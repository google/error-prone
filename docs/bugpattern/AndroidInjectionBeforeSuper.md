Members injection should always be called as early as possible to avoid
uninitialized @Inject members. This is also crucial to protect against bugs
during configuration changes and reattached Fragments to make sure that each
framework type is injected in the appropriate order.
