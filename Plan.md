Natuurlijk! Op basis van de architectuur in het `README.md`-bestand, is hier een volledige uitleg van hoe de EmpireWand functioneert en welke commando's je kunt gebruiken.

### Kernfuncties van de EmpireWand

De plugin is ontworpen om de EmpireWand een uniek en op zichzelf staand magisch item te maken. Dit zijn de belangrijkste functies:

1.  **Item-Gebonden Magie:**
    * Alle spreuken die je op een wand bindt, worden direct op dat specifieke item opgeslagen. 
    * Dit betekent dat je meerdere EmpireWands in je inventory kunt hebben, elk met een eigen set spreuken. De ene kan bijvoorbeeld alleen `Leap` hebben voor mobiliteit, terwijl een andere een volledig arsenaal aan aanvalsspreuken heeft.
    * Omdat de data op het item staat, kun je de wand veilig opbergen in een kist of aan een andere speler geven, zonder dat de spreuken verloren gaan.

2.  **Besturing van de Wand:**
    * **Linker muisknop:** Voert de momenteel geselecteerde spreuk uit.
    * **Rechter muisknop:** Wisselt naar de **volgende** spreuk in de lijst die op de wand gebonden is. Je krijgt een bericht in je action bar te zien welke spreuk nu actief is.
    * **Sneak + Rechter muisknop:** Wisselt naar de **vorige** spreuk in de lijst.

3.  **Modulair Systeem:**
    * De kern van de plugin is zo gebouwd dat spreuken en wands los van elkaar staan. Dit betekent dat in de toekomst nieuwe wands (zoals een `MephiWand`) dezelfde spreuken als de EmpireWand kunnen gebruiken, zoals `Comet` of `Leap`, zonder dat de code herschreven hoeft te worden.

### Commando's voor de EmpireWand

Alle commando's voor de EmpireWand vallen onder het hoofdcommando `/ew` (afkorting voor EmpireWand).

#### **`/ew empirewand`**
* **Functie:** Geeft je een nieuwe, lege EmpireWand. Dit is het basisitem waarop je vervolgens spreuken kunt binden.
* **Permissie (suggestie):** `empirewand.get`

#### **`/ew bind [spreuk]`**
* **Functie:** Bindt een specifieke spreuk aan de EmpireWand die je in je hand hebt.
* **Gebruik:** Houd de EmpireWand vast en typ `/ew bind comet`. De spreuk "Comet" wordt dan toegevoegd aan de lijst van beschikbare spreuken op die wand.
* **Tab-Completion:** Als je `/ew bind` typt en op `Tab` drukt, krijg je een lijst te zien van **alle** beschikbare spreuken die je op de wand kunt zetten.
* **Permissie (suggestie):** `empirewand.bind`

#### **`/ew unbind [spreuk]`**
* **Functie:** Verwijdert een specifieke spreuk van de EmpireWand die je in je hand hebt.
* **Gebruik:** Als je wand "Comet" en "Leap" heeft, kun je `/ew unbind leap` typen om alleen "Leap" te verwijderen.
* **Tab-Completion:** De tab-completion voor dit commando is slim: het toont alleen de spreuken die **daadwerkelijk op de wand zitten** die je vasthoudt.
* **Permissie (suggestie):** `empirewand.unbind`

#### **`/ew bindall`**
* **Functie:** Een handig commando om in één keer **alle** beschikbare spreuken in de plugin aan de EmpireWand in je hand te binden.
* **Gebruik:** Ideaal voor admins of voor testdoeleinden.
* **Permissie (suggestie):** `empirewand.bindall`

Dit systeem geeft spelers de volledige vrijheid om hun eigen, unieke magische staf samen te stellen, precies zoals ze dat willen.