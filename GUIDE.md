# How to organize event

1. Create the basic event
    1. ```/event create <name>```
    1. ```/event set track <track>```
    
    > When you create an event, the plugin automatically selects it for you. But if you were to restart the server you need to choose what event you are doing with ```/event select <name>```.

1. Create heats in the event
    1. ```/heat create qualy```
        1. ```/heat set time <seconds> <heatname>```
        1. ```/heat set startdelay <seconds> <heatname>```
        1. ```/heat add driver <playername> <heatname>``` or ```/heat add alldrivers <qualyheat>```
        > The drivers will start in the order they are added. If add alldrivers are used I'm not sure what order it will be.
    1. ```/heat create final```
        1. ```/heat set laps <laps> <heatname>```
        1. ```/heat set pits <laps> <heatname>```
        >If you run qualifications, make sure to not add drivers to the final heat. If you are not using qualification, you can add drivers to the final heat directly. They will start in order they are added.

1. Begin the event with ```/event start```
1. Run qualification
    1. ```/heat load <heatname>```
    1. ```/heat start <heatname>```
    1. If you need to reset the heat. You can do it before the heat is finished by doing ```/heat reset <heatname>```
    1. if someone doesn't finish the heat, you can end it early by doing ```/heat finish <heatname>```
    1. **After the heat is finished, make sure you wait the 10 seconds until the scoreboard disappears to proceed to the next heat.**
1. When all qualification heats are finished. Execute ```/event finish qualifications```. This will automatically populate the drivers into the final heat.
1. Run final the same way as you did the qualification in step 4.
1. After finals are done. Make sure you do ```/event finish finals``` to mark the event as finished.
1. Event is completed and you can look at the results with /event results finals
            
