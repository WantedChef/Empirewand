# EmpireWand Changelog

## [1.0.0] - 2025-09-05

### Added
- **Core Features**: Complete wand system with PDC-based data persistence
- **Commands**: `/ew get`, `/ew bind`, `/ew unbind`, `/ew bindall`, `/ew list`, `/ew reload`
- **Spell System**: 10 spells implemented (Leap, Comet, Explosive, MagicMissile, GlacialSpike, GraspingVines, Heal, LifeSteal, Polymorph, EtherealForm)
- **Configuration**: YAML-based config system (`config.yml`, `spells.yml`) with runtime reloading
- **Permissions**: Comprehensive permission matrix for commands and spells
- **Game Feel**: Action bar feedback, sounds, particles, and cooldown system
- **Listeners**: Wand interaction, entity hits, projectile handling

### Technical
- **Java 21** compatibility
- **Paper 1.20.6** target
- **Modular Architecture**: Services, registries, and listeners
- **PDC Data Model**: Persistent wand data with version migration support
- **Debounce & Cooldown**: Per-player rate limiting and spell cooldowns

### Documentation
- Complete README with installation and configuration
- Technical documentation (`technical.md`)
- Guidelines and roadmap (`guidelines.md`, `roadmap.md`)
- Step-by-step plan (`stappenplan.md`)

### Build
- Gradle build system
- Successful compilation and testing
- JAR generation ready for deployment

### Known Issues
- Unit tests not yet implemented
- Performance monitoring not fully integrated
- Some edge cases may need additional hardening

### Future Plans
- Additional spells and features
- Performance optimizations
- Third-party integrations (WorldGuard, bStats, etc.)