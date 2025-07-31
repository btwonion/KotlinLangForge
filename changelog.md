### Add automatic event registration
To use automatic event listener registration the `@EventBusSubscriber` annotation has to be added on the class/file.
Additionally, you can annotate a method with `SubscribeEvent` to adjust the listener's parameters.\
This is not necessary though!
Klf automatically looks for events in every method inside of the class and automatically
determines which event bus to use.

**Note for Forge developers:** Private event listeners cannot be processed on Forge and will result in a crash!