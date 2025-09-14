#!/bin/bash

# Script to fix checkstyle star import warnings

# Fix WandSelectorMenuTest unused imports
sed -i '/import java.util.Set;/d' src/test/java/nl/wantedchef/empirewand/gui/WandSelectorMenuTest.java
sed -i '/import org.mockito.ArgumentMatchers.any;/d' src/test/java/nl/wantedchef/empirewand/gui/WandSelectorMenuTest.java
sed -i '/import org.mockito.ArgumentMatchers.anyFloat;/d' src/test/java/nl/wantedchef/empirewand/gui/WandSelectorMenuTest.java

# Fix EnergyShieldTest unused imports
sed -i '/import nl.wantedchef.empirewand.spell.Spell;/d' src/test/java/nl/wantedchef/empirewand/spell/enhanced/EnergyShieldTest.java
sed -i '/import org.mockito.Mockito.any;/d' src/test/java/nl/wantedchef/empirewand/spell/enhanced/EnergyShieldTest.java
sed -i '/import org.mockito.Mockito.anyLong;/d' src/test/java/nl/wantedchef/empirewand/spell/enhanced/EnergyShieldTest.java
sed -i '/import org.mockito.Mockito.verify;/d' src/test/java/nl/wantedchef/empirewand/spell/enhanced/EnergyShieldTest.java

# Fix MeteorShowerTest unused import
sed -i '/import nl.wantedchef.empirewand.spell.Spell;/d' src/test/java/nl/wantedchef/empirewand/spell/enhanced/MeteorShowerTest.java

echo "Fixed unused imports"