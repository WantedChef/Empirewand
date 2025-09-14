# EmpireWand Restructure Plan

## New Spell Folder Structure (Max 6 files per folder)

### Fire Magic (Split into 3 folders)
**fire/basic/** (6 files):
- Fireball.java (consolidated with FireballEnhanced)
- Comet.java (consolidated with CometEnhanced)
- Burn.java
- Lava.java
- BlazeLaunch.java
- Explosive.java

**fire/advanced/** (6 files):
- CometShower.java
- EmpireComet.java
- Inferno.java
- FlameWave.java
- MagmaWave.java
- Flamewalk.java

**fire/effects/** (2 files):
- ExplosionTrail.java

### Lightning Magic (Split into 2 folders)
**lightning/basic/** (6 files):
- ChainLightning.java (consolidated with ChainLightningEnhanced)
- LightningBolt.java (consolidated with LightningBoltRefactored)
- LightningStorm.java
- Spark.java
- LittleSpark.java
- ThunderBlast.java

**lightning/advanced/** (6 files):
- ElementalLightningAttack.java
- LightningArrow.java
- LightningWave.java
- SolarLance.java

### Movement Magic (Split into 3 folders)
**movement/teleport/** (6 files):
- Teleport.java (consolidated with TeleportEnhanced)
- ElementalTeleporter.java
- Phase.java
- Recall.java
- EmpireEscape.java
- BlinkStrike.java

**movement/flight/** (6 files):
- Levitate.java
- Lift.java
- Rocket.java
- Leap.java
- StellarDash.java
- SunburstStep.java

### Healing Magic
**heal/** (6 files):
- Heal.java (consolidated with HealEnhanced)
- DivineHeal.java
- SuperHeal.java

### Weather Magic
**weather/** (6 files):
- Blizzard.java (consolidated from weather/Blizzard + enhanced/BlizzardEnhanced)
- MeteorShower.java (consolidated from enhanced/MeteorShower + enhanced/MeteorShowerEnhanced)

### Enhanced Spells (Redistribute)
**enhanced/cosmic/** (6 files):
- BlackHole.java
- VoidZone.java
- TimeDilation.java
- TemporalStasis.java
- GravityWell.java
- HomingRockets.java

**enhanced/defensive/** (6 files):
- DivineAura.java
- EnergyShield.java
- StoneFortress.java

**enhanced/destructive/** (6 files):
- DragonsBreath.java
- LightningStorm.java
- ShockwaveRafg.java
- EnhancedWaveSpell.java

## Files to Remove/Consolidate:
1. ChainLightningRefactored.java (merge with ChainLightning.java)
2. LightningBoltRefactored.java (merge with LightningBolt.java)
3. All *Enhanced.java files (merge with base versions)
4. EnhancedEmpireWandCore.java (marked deprecated)
5. EnhancedConfigService.java (consolidate with ConfigService)

## Consolidation Strategy:
Each base spell will support enhancement levels through enum parameters:
```java
public enum EnhancementLevel {
    STANDARD(1.0, "Standard version"),
    ENHANCED(1.5, "Enhanced with improved effects"),
    ULTIMATE(2.0, "Ultimate power level")
}
```

This reduces code duplication and maintains all functionality in single files.