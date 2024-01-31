# Skate-Routing-App
Machen Sie Ihre nächste Fahrt oder Ihren nächsten Ausflug mit der Skate-Routing-App zu einem Abenteuer. Möchte man zum Skatepark fahren oder einen kleinen Ausflug machen und beliebige Strecken mit dem Skateboard fahren dann ist diese Navigation App eine große Hilfe. Der Wunsch sein Zielort mit Leichtigkeit zu erreichen, kann mit diesem Routingprofil möglich gemacht werden. Was viele nicht wissen: Ist man mit dem Skateboard/Longboard/Rollerskate unterwegs, gelten die selben Regelungen wie für Fußgänger. Aus diesem Grund ist der Gehweg zu nutzen. [Vorschrift der Straßenverkehrs-Ordnung (StVO), Unter § 24 Abs. 1 StVO ]

Diese App berücksichtigt zur Routenerstellung nicht nur die Regeln der Straßenverkehrs-Ordnung für Skateboards, sondern auch physische Faktoren wie Oberfläche, Beschaffenheit, mögliche Barrieren uvm.



## Skate-Routing-App Video Resultat in Android Studio

https://github.com/IoannisSvolosBht/Skate-Routing-App/assets/124213124/13ef6138-0d58-4b2d-9f76-7a32d6cf7ed0


# SkateFlagEncoder 
[core/src/main/java/com/graphhopper/routing/util/SkateFlagEncoder.java](core/src/main/java/com/graphhopper/routing/util/SkateFlagEncoder.java)



# Erste Schritte in Android Studio oder am Android-Smartphone

* Unterstützte Android-API-Level sind: 22–30


* [berlin-gh Graph Ordner](https://drive.google.com/drive/folders/1f0TnXo6IR2YehuK_q4PeIiljeIEktcyi?usp=drive_link) (berlin.map, nodes, edges etc.) Ordner muss im Device Explorer kopiert werden unter: ```/sdcard/download/graphhopper/maps/ ```
  
   Device Explorer            |
   :-------------------------:|
   ![import-map-sdcard](https://github.com/IoannisSvolosBht/Skate-Routing-App/assets/124213124/c2d8c3ff-6ee7-4a3e-89b5-88e9b1f23cd3) |


# So wurde der Berlin-Graph erstellt

  1. [Download openstreetmap file](https://download.geofabrik.de/europe/germany/berlin.html)
  2. [config-example.yml](config-example.yml) anpassen - wird im nächsten Schritt vom scriptfile [graphhopper.sh](./graphhopper.sh) verwendet um FlagEncoder, Graph bytes und Encoded Values zu bestimmen

    config-example.yml Datei
  
     ```
     graphhopper:
     OpenStreetMap input file
     datareader.file: some.pbf

     graph.flag_encoders: skate

     graph.encoded_values: road_class,road_class_link,road_environment,max_speed,road_access,surface,smoothness
     If many flag_encoders or encoded_values are used you need to increase bytes_for_flags to 8 or more (multiple of 4)
     graph.bytes_for_flags: 8
     
    ```  

  3. ``` ./graphhopper.sh -a import -i <openstreetmapfile> ``` ausführen. Das erstellt die Routing Daten
  4. berlin-gh wurde nun erstellt (nodes, edges etc.)
  5. [Download a map berlin.map](http://download.mapsforge.org/maps/)
  6. berlin.map (Grundkarte) im gerade erstellten berlin-gh Ordner kopieren
  7. berlin-gh Ordner muss nun im Device Explorer kopiert werden unter: ```/sdcard/download/graphhopper/maps/ ```




# Map & Routing Engine
* Grundkarte berlin.map von Mapsforge VTM Render Engine [Download a map](http://download.mapsforge.org/maps/)
  
* OSM file berlin-latest.oms.pbf um Routing Daten zu erstellen von Geofabrik [Download openstreetmap file](https://download.geofabrik.de/europe/germany/berlin.html)

* Routing Engine von GraphHopper [Graphhopper 0.13.0](https://github.com/graphhopper/graphhopper/tree/0.13). Die App basiert grundsätzlich auf der Version Graphhopper 0.13.0. Beim erstellen des SkateFlagEncoders wurden auch einige Klassen aus höheren Versionen manuell hinzugefügt wie z.B. EncodingValue Smoothness (bei Graphhopper erst ab Version 1.0).




    
