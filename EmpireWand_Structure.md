EmpireWand Plugin - Definitieve Architectuur
1. Visie
Een volledig modulair en schaalbaar wand-systeem. We beginnen met een perfecte recreatie van de EmpireWand en bouwen een fundament waarop in de toekomst eenvoudig nieuwe wands (MephiWand, BloodWand, etc.) en spreuken kunnen worden toegevoegd. De architectuur is erop gericht dat elke component onafhankelijk kan functioneren, waardoor het mogelijk wordt voor andere ontwikkelaars om in de toekomst "spreuken-packs" of "wand-addons" te creëren die naadloos integreren met de kern van de plugin, zonder dat er aanpassingen in de broncode nodig zijn.
Het uiteindelijke doel is een ecosysteem waar de community kan bijdragen aan de magie van de server.
2. Kernprincipes & Moderne Praktijken
Java 21 & Paper API 1.20.6: We gebruiken bewust de laatste standaarden. Java 21 biedt significante performanceverbeteringen en moderne taalfeatures die de code beknopter en beter leesbaar maken. De Paper API is de de-facto standaard voor high-performance servers; het biedt talloze optimalisaties en een rijkere, modernere API in vergelijking met oudere platformen zoals Spigot, wat resulteert in een stabielere en snellere plugin.
Volledig Modulair: De architectuur is opgebouwd rondom een strikte scheiding van verantwoordelijkheden. De core (motor) weet niets over specifieke spreuken of wands; het biedt enkel de generieke functionaliteit voor het opslaan en uitvoeren van magie. De spells (magie) zijn volledig op zichzelf staande effecten die niets weten over welke wand ze gebruikt. De wands (items) zijn de implementatielaag die een fysiek item koppelt aan een set van beschikbare spreuken. Deze ontkoppeling is cruciaal voor onderhoudbaarheid en uitbreidbaarheid.
Package-per-Spell Structuur: Elke spreuk bevindt zich in zijn eigen package (spells/comet/, spells/leap/). Hoewel dit voor simpele spreuken overdreven kan lijken, is het een bewuste keuze voor toekomstbestendigheid. Zodra een spreuk complexer wordt en bijvoorbeeld een eigen Listener nodig heeft (voor een projectiel dat een speciaal effect heeft bij impact) of een eigen configuratieklasse, staan al deze gerelateerde bestanden netjes georganiseerd in één map. Dit voorkomt een onoverzichtelijke codebase naarmate de plugin groeit.
Persistent Data Container (PDC): Alle data (zoals de lijst van gebonden spreuken en de momenteel actieve spreuk) wordt direct op de ItemStack zelf opgeslagen via de ingebouwde PDC-API van Paper. Dit is superieur aan alternatieven zoals data opslaan in een centrale HashMap, omdat de data inherent aan het item verbonden is. Hierdoor verliest een wand zijn magie niet bij een server-herstart, kan hij veilig worden verhandeld tussen spelers, en kunnen er meerdere unieke wands tegelijk in het spel bestaan, elk met hun eigen configuratie.
Kyori Adventure & MiniMessage: We vermijden de verouderde &-kleurcodes volledig. Het Kyori Adventure-framework is de moderne standaard voor alle tekst in Minecraft. Met MiniMessage kunnen we op een intuïtieve, HTML-achtige manier complexe tekstcomponenten bouwen, inclusief gradients, hover-events en click-events. Dit maakt het niet alleen voor de ontwikkelaar makkelijker om de UI te beheren, maar biedt ook een veel rijkere ervaring voor de speler.
Cloud Command Framework: Het standaard Bukkit-commandosysteem is rigide en vereist veel handmatige validatie. Cloud is een modern, krachtig framework dat ons in staat stelt om complexe commando's te definiëren met minimale code. Het biedt features zoals asynchrone command-uitvoering, intelligente en context-bewuste tab-completion (bijv. /ew unbind toont alleen spreuken die daadwerkelijk op de wand zitten), en robuuste argument-parsing, wat de gebruikerservaring aanzienlijk verbetert.
Dependency Injection: In plaats van overal statische getInstance()-methodes te gebruiken, passen we Dependency Injection toe. De hoofdklasse (EmpireWandPlugin) creëert de benodigde objecten (zoals WandDataHandler) en "injecteert" deze via de constructor in de klassen die ze nodig hebben. Dit leidt tot een sterk ontkoppelde codebase, maakt de afhankelijkheden van een klasse expliciet, en maakt het schrijven van unit-tests exponentieel eenvoudiger omdat we met "mock"-objecten kunnen werken.
3. Definitieve Mappenstructuur
src/main/java/nl/dusdavidgames/kingdom/empirewand/
├── EmpireWandPlugin.java         // Hoofdklasse. Verantwoordelijk voor de 'setup' en 'teardown' van de plugin. Registreert listeners, commands en initialiseert alle kerncomponenten.

├── core/                         // De agnostische 'motor' van de plugin. Deze code weet niets van een 'EmpireWand' of 'CometSpell'.
│   ├── listeners/
│   │   ├── ProjectileListener.java // Luistert naar globale projectiel-events en delegeert de logica als het projectiel een magische 'tag' heeft.
│   │   └── WandInteractionListener.java // Vangt alle interacties (links/rechts-klik) op. Controleert of het item een wand is en delegeert naar de juiste actie (spreuk uitvoeren of wisselen).
│   └── wand/
│       └── data/
│           ├── WandDataHandler.java // De enige klasse die direct communiceert met de PDC van een ItemStack. Bevat alle logica voor het lezen, schrijven en updaten van wand-data.
│           └── WandKeys.java     // Een statische 'container' voor alle NamespacedKey-objecten om typefouten te voorkomen en consistentie te garanderen.

├── spells/                       // Gecentraliseerde locatie voor ALLE spreuken. Spreuken zijn onafhankelijk en herbruikbaar.
│   ├── Spell.java                // Het contract/interface waar elke spreuk aan moet voldoen. Definieert de basisfunctionaliteit zoals `execute()`.
│   ├── SpellRegistry.java        // Een singleton die een map beheert van alle geregistreerde spreuken. Wordt gebruikt om een spreuk op te zoeken op basis van zijn interne naam.
│   ├── comet/
│   │   └── CometSpell.java       // De concrete implementatie van de Comet-spreuk. Bevat alle logica voor deze specifieke spreuk.
│   └── leap/
│       └── LeapSpell.java        // De concrete implementatie van de Leap-spreuk.

├── wands/                        // Specifieke implementaties van elke wand. Hier wordt de 'core' en 'spells' samengebracht tot een concreet item.
│   ├── Wand.java                 // Een interface die de eigenschappen van een wand-type definieert, zoals de naam, het materiaal en de lore.
│   ├── WandRegistry.java         // Beheert een lijst van alle bekende wand-types, zodat de plugin weet welke wands er bestaan.
│   └── empire/
│       ├── EmpireWand.java       // De concrete implementatie die de `Wand`-interface invult voor de EmpireWand.
│       └── EmpireWandCommands.java // Regelt alle sub-commands die specifiek zijn voor de EmpireWand, zoals het binden van spreuken en het verkrijgen van de wand.
│
└── util/                         // Algemene utility classes die overal in de plugin gebruikt kunnen worden.
    └── ComponentUtil.java        // Een kleine helper-klasse om het werken met MiniMessage nog eenvoudiger te maken.


er komen nog veel meer spells bij leap en comet zijn even voorbeeld.