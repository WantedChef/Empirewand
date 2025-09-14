Purpose: This file is the single source of truth describing each specialist agent in the orchestra, how the plugin-dev-orchestra-conductor selects them, and what inputs/outputs and checks are required to ship.

0) How to use this file

Conductor-only selection: The plugin-dev-orchestra-conductor is responsible for selecting exactly one specialist for a task, coordinating hand‑offs if needed.

Smallest Logical Diff: Favor minimal, targeted changes that satisfy the task without refactors unless explicitly requested.

Approval Gates: Use Step 1 → Step 2 → Step 3 gates defined below for every task.

Deliverable Format: All changes must be delivered as diff -u blocks per file, plus tests and verification commands.

Standard Task Flow

Step 1 — Selection & Plan (Conductor)

Select the specialist.

Provide a short rationale, a bullet plan (files to touch/create), and any clarifying questions.

Step 2 — Implementation (Specialist, coordinated by Conductor)

Provide final patches (diff -u), tests, and run instructions.

Step 3 — Completion

Document assumptions & edge cases; propose Conventional Commit messages.

1) Agent Schema (applies to all agents)

Name — exact filename in /agents/ (without extension)

Domain — concise scope of authority

Primary Responsibilities — what this agent designs/implements

Select When — concrete triggers/signals

Inputs — required context/files

Outputs — patches, tests, artifacts

Handoffs — typical downstream collaborators

Risks/Checks — pitfalls + pre‑merge checklist

KPIs — measurable outcomes (perf, coverage, complexity)

Unless otherwise noted, language is Java 21, package root is nl.wantedchef.empirewand, and code must respect repo guidelines.

2) Selection Matrix (quick guide)
Need	Pick
New /command or subcommand, parsing, permissions	minecraft-command-architect
GUI menus/inventories, UX flows	minecraft-gui-ux-architect
Data model, repositories, caching	minecraft-data-architect or data-architecture-specialist (non-MC)
Paper API/plugin lifecycle, plugin.yml, services	papermc-plugin-architect-pro
Performance issues, profiling, GC, hot paths	minecraft-performance-master (MC) / performance-optimization-master (general)
Threads, scheduler, async I/O	minecraft-scheduler-threading-expert (MC) / concurrency-threading-expert (general)
Config & i18n/messages	minecraft-config-localization-expert (MC) / configuration-i18n-expert (general)
DB migrations	minecraft-database-migration-master
Security/permissions/abuse/anticheat	minecraft-security-anticheat-master (MC) / security-specialist (general)
Events/bus design	bukkit-event-system-master (Paper/Bukkit) / event-system-master (general)
Integrating multiple plugins/APIs	minecraft-plugin-integration-architect
Cross‑service or external systems	integration-architect / system-integration-architect
DevOps: build, CI/CD, packaging	minecraft-devops-deployment-expert (MC) / devops-deployment-expert
Analytics/metrics/telemetry	minecraft-metrics-analytics-expert
Economy, currency hooks	minecraft-economy-master
Entities/NPCs/pathfinding	minecraft-entity-npc-master
Particles/holograms/displays	minecraft-particle-effect-specialist, minecraft-hologram-display-expert
Protocol/packets	minecraft-network-protocol-expert (MC) / network-protocol-expert
Minigames/framework	minecraft-minigame-framework-master
World generation/regions/protection	minecraft-world-architect, minecraft-region-protection-expert
Legacy migration	minecraft-legacy-migration-expert
CLI tooling	cli-framework-architect
Testing strategy & harness	minecraft-testing-master (MC) / testing-framework-master
Overall architecture	code-architect, software-architect-pro
End‑to‑end project orchestration	plugin-dev-orchestra-conductor / development-orchestra-conductor (cross‑repo)
3) Agents (detailed)

The following entries are comprehensive and complete. Each contains detailed domain expertise, responsibilities, selection signals, and ready‑to‑ship checklists.

plugin-dev-orchestra-conductor

Domain: End-to-end task coordination for complex plugin development projects with multiple specialist teams. Primary Responsibilities: Master coordinator for complex plugin development, managing multiple specialists, architectural decisions, ensuring cohesive development across large-scale Minecraft plugin ecosystems, technical leadership, and project orchestration. Select When: Any new task enters; complex multi-component features; multi-team integration; conflict resolution; architectural decisions spanning multiple domains. Inputs: Task description, repo guidelines, current code, team contexts, integration requirements. Outputs: Comprehensive step-1 plan with specialist selection; integrated patches; final reports; handoff coordination; quality gates. Handoffs: All specialists based on task requirements. Risks/Checks: Scope creep; conflicting edits; integration failures; missed dependencies; coordination overhead. Checklist: Single specialist per task phase; smallest logical diff; comprehensive tests; repo rules respected; proper handoffs executed. KPIs: Lead time to PR; integration defects; specialist utilization efficiency.

papermc-plugin-architect-pro

Domain: Elite PaperMC 1.20.6 plugin architecture with deep expertise in modern Paper API, Mojang mappings, plugin lifecycle, dependency injection, and enterprise-grade architecture patterns. Primary Responsibilities: Modern plugin bootstrap with enterprise architecture; service-oriented architecture with async operations; dependency injection patterns; paperweight-userdev integration; plugin lifecycle management; module boundaries and service wiring. Select When: New plugin components; service architecture design; Paper API upgrades; dependency injection setup; enterprise architecture patterns; lifecycle hook implementation. Inputs: Existing code structure, plugin.yml, dependency requirements, service specifications. Outputs: Cohesive module layout with enterprise patterns; DI setup; lifecycle-safe code; service abstractions; async operation patterns. Handoffs: Command, GUI, Data, Integration specialists. Risks/Checks: Paper API must be compileOnly; avoid shading Paper; validate startup/shutdown sequences; service lifecycle management. KPIs: Clean startup logs; zero illegal access warnings; service initialization time; dependency resolution efficiency.

minecraft-command-architect

Domain: Master of Minecraft command systems with expertise in brigadier integration, Paper's enhanced command API, complex command trees, argument validation, and enterprise-grade command frameworks for 1.20.6. Primary Responsibilities: Complex command systems with validation; interactive command workflows; permission systems; async execution support; argument parsing and validation; command tree architecture; tab completion systems. Select When: New /command or subcommand implementation; complex argument validation; interactive workflows; permission integration; async command processing; brigadier integration. Inputs: Command specifications, permission model, validation requirements, localization keys, workflow definitions. Outputs: Command handlers with validation; interactive workflows; permission checks; comprehensive help/usage systems; tab completion logic. Handoffs: Config/i18n, Data, Security specialists. Risks/Checks: No blocking I/O on main thread; command injection prevention; preserve message keys; comprehensive unit tests for parsing and validation. KPIs: Command execution latency; permission gate correctness; validation accuracy; user experience metrics.

minecraft-gui-ux-architect

Domain: Master of Minecraft user interface design specializing in advanced inventory GUIs, interactive menus, accessibility, user experience optimization, and cross-platform compatibility with Adventure API expertise. Primary Responsibilities: Multi-page inventory systems with dynamic layouts; interactive GUI components; accessibility features; real-time updates; user experience optimization; cross-platform compatibility; Adventure API integration. Select When: Complex inventory GUI systems; interactive components; accessibility requirements; dynamic layouts; real-time UI updates; user experience optimization. Inputs: UI wireframes, item icons, interaction specifications, accessibility requirements, dynamic content needs. Outputs: GUI classes with dynamic layouts; interactive components; accessibility features; state synchronization; real-time update systems. Handoffs: Data, Metrics, Config specialists. Risks/Checks: Prevent item duplication; handle close events properly; accessibility compliance; UI manipulation security; performance optimization. KPIs: Interaction success rate; zero duplication bugs; accessibility compliance score; UI responsiveness metrics.

minecraft-data-architect

Domain: Elite data management expert for Minecraft plugins specializing in advanced database design, caching strategies, data synchronization, migration systems, and high-performance data operations for large-scale servers. Primary Responsibilities: Multi-tier caching systems; cross-server data synchronization; advanced database design; data migration frameworks; high-performance data operations; conflict resolution; consistency guarantees. Select When: Complex data architecture; multi-tier caching needs; cross-server synchronization; high-performance data operations; data migration requirements; consistency challenges. Inputs: Data entities, database schema, caching requirements, synchronization needs, migration plans. Outputs: Multi-tier caching systems; synchronization mechanisms; database architecture; migration frameworks; conflict resolution systems. Handoffs: Database Migration, Analytics, Security specialists. Risks/Checks: Thread safety; data consistency; write amplification; cache coherence; synchronization conflicts. KPIs: Cache hit rates; P95 data latency; synchronization success rate; consistency validation metrics.

minecraft-database-migration-master

Domain: Expert in database migrations, schema evolution, data transformations, and zero-downtime database operations for Minecraft plugin development. Primary Responsibilities: Zero-downtime migration systems; schema evolution with backward compatibility; data transformation pipelines; rollback capabilities; migration automation; version control integration. Select When: New database schema changes; data model evolution; zero-downtime requirements; complex data transformations; migration automation needs. Inputs: Proposed schema changes, data transformation requirements, compatibility constraints. Outputs: Migration scripts with rollback capabilities; schema evolution systems; data transformation pipelines; migration automation tools. Handoffs: Data Architect, DevOps specialists. Risks/Checks: Online migration safety; rollback procedures tested; data integrity verification; migration atomicity. KPIs: Zero migration downtime; rollback success rate; data integrity validation; migration performance metrics.

minecraft-scheduler-threading-expert

Domain: Advanced concurrency specialist focusing on Minecraft's scheduler, async operations, thread safety, and performance optimization for multi-threaded plugin development. Primary Responsibilities: Advanced async operations with CompletableFuture; repeating task management with dynamic intervals; thread-safe data structures; performance optimization; scheduler integration; concurrency pattern implementation. Select When: Long-running async tasks; thread safety issues; scheduler optimization; concurrency bugs; performance bottlenecks related to threading; CompletableFuture integration. Inputs: Call graphs, performance traces, concurrency requirements, scheduler specifications. Outputs: Async operation frameworks; thread-safe data structures; scheduler optimizations; concurrency patterns; performance monitoring. Handoffs: Performance Master, Security specialists. Risks/Checks: No world access off main thread; deadlock prevention; race condition testing; resource leak prevention. KPIs: Main-thread time reduction; async operation success rate; thread safety validation; concurrency performance metrics.

minecraft-performance-master

Domain: Elite performance optimization specialist for Minecraft plugins with expertise in profiling, memory management, async operations, JVM tuning, and scalability for high-performance servers running Paper 1.20.6. Primary Responsibilities: TPS optimization and profiling; memory management with JVM tuning; performance monitoring systems; async operation optimization; scalability improvements; bottleneck identification and resolution. Select When: TPS drops; lag spikes; memory issues; scalability challenges; performance bottlenecks; JVM optimization needs. Inputs: Performance profiles, timing reports, memory analysis, scalability requirements. Outputs: Performance optimization systems; memory management frameworks; TPS monitoring; JVM tuning configurations; scalability solutions. Handoffs: Scheduler, Architecture, DevOps specialists. Risks/Checks: Performance correctness preserved; regression prevention; memory leak detection; TPS impact validation. KPIs: TPS stability; P99 latency reduction; memory efficiency; scalability metrics.

performance-optimization-master

Domain: Elite performance optimization specialist with expertise in profiling, memory management, async operations, runtime tuning, and scalability for high-performance applications across all programming languages and platforms. Primary Responsibilities: Advanced performance profiling with real-time monitoring; memory optimization with object pooling; async operation frameworks; runtime optimization; scalability patterns; performance monitoring integration. Select When: Performance bottlenecks across any technology stack; memory optimization needs; async performance issues; scalability challenges; real-time monitoring requirements. Inputs: Performance profiles, memory analysis, scalability requirements, monitoring specifications. Outputs: Performance monitoring systems; memory optimization frameworks; async operation patterns; scalability solutions; real-time alerting systems. Handoffs: Concurrency, Architecture, DevOps specialists. Risks/Checks: Resource exhaustion prevention; DoS attack mitigation; performance regression testing; monitoring accuracy. KPIs: System throughput; memory efficiency; response time percentiles; scalability metrics.

minecraft-config-localization-expert

Domain: Advanced configuration management and internationalization specialist focusing on complex config systems, multi-language support, and dynamic configuration for Minecraft plugins. Primary Responsibilities: Hierarchical configuration systems with validation; multi-language support with dynamic switching; hot-reloading capabilities; configuration migration; validation frameworks; i18n implementation with pluralization. Select When: Complex configuration requirements; multi-language support; hot-reloading needs; configuration validation; i18n implementation; dynamic configuration changes. Inputs: Configuration specifications, localization requirements, validation rules, migration needs. Outputs: Configuration frameworks with validation; i18n systems with pluralization; hot-reload mechanisms; migration tools; validation systems. Handoffs: Command, GUI, Security specialists. Risks/Checks: Configuration injection prevention; unauthorized settings access; key preservation; placeholder integrity. KPIs: Zero missing key errors; configuration reload success rate; validation accuracy; localization coverage.

configuration-i18n-expert

Domain: Master of application configuration management and internationalization (i18n) with expertise in environment-specific configs, localization, multi-language support, and configuration automation across all technology stacks. Primary Responsibilities: Environment-specific configuration management; comprehensive i18n support with RTL languages; configuration validation and automation; secrets management; multi-environment support; localization workflow automation. Select When: Complex configuration systems across multiple environments; comprehensive i18n requirements; configuration automation; secrets management; localization workflow optimization. Inputs: Configuration requirements, environment specifications, localization needs, security constraints. Outputs: Multi-environment configuration systems; comprehensive i18n frameworks; configuration automation; secrets management integration; localization tools. Handoffs: Security, DevOps, Integration specialists. Risks/Checks: Secrets exposure prevention; configuration injection attacks; environment isolation; i18n security considerations. KPIs: Configuration accuracy across environments; localization coverage; secrets management compliance; automation efficiency.

minecraft-plugin-integration-architect

Domain: Expert in multi-plugin compatibility, API design, dependency management, and creating seamless integration between different Minecraft plugins and external services. Primary Responsibilities: Plugin-to-plugin communication with API design; external service integration; dependency management; compatibility layers; API versioning; integration testing frameworks. Select When: Multi-plugin integration needs; external service integration; API design requirements; dependency management challenges; compatibility issues; integration testing. Inputs: Integration specifications, API requirements, dependency constraints, compatibility matrices. Outputs: Integration frameworks; API design patterns; dependency management systems; compatibility layers; integration testing suites. Handoffs: Security, Network, Database specialists. Risks/Checks: Unauthorized access prevention; API versioning compatibility; dependency conflicts; integration security validation. KPIs: Integration success rates across versions; API compatibility metrics; dependency resolution efficiency; external service reliability.

integration-architect

Domain: Master of system integration patterns, API design, microservices communication, event-driven architectures, and enterprise integration solutions across all technology stacks and platforms. Primary Responsibilities: System integration architecture with error handling; message broker systems with dead letter queues; API gateway design; microservices communication patterns; event-driven architectures; enterprise integration patterns. Select When: Complex system integration requirements; microservices architecture; message broker implementation; API gateway design; event-driven systems; enterprise integration challenges. Inputs: Integration requirements, system architecture, communication patterns, reliability specifications. Outputs: Integration architectures; message broker systems; API gateway implementations; event-driven frameworks; enterprise integration solutions. Handoffs: Security, Network, Performance specialists. Risks/Checks: Integration security validation; message delivery guarantees; circuit breaker implementation; error handling completeness. KPIs: Integration reliability; message throughput; API response times; system availability metrics.

system-integration-architect

Domain: System Integration Architect specializing in designing and implementing complex distributed systems, service architectures, and integration patterns across diverse technology stacks. Primary Responsibilities: Distributed system design; service mesh architecture; data flow architecture; system topology optimization; enterprise integration patterns; cloud-native integration solutions. Select When: Complex distributed system design; service mesh implementation; enterprise integration requirements; cloud-native architecture; cross-platform integration challenges. Inputs: System requirements, architecture specifications, integration patterns, scalability needs. Outputs: Distributed system architectures; service mesh implementations; integration pattern libraries; cloud-native solutions; system documentation. Handoffs: Security, Performance, DevOps specialists. Risks/Checks: System security validation; integration point monitoring; service dependency management; scalability testing. KPIs: System reliability; integration performance; service availability; scalability metrics.

minecraft-security-anticheat-master

Domain: Comprehensive security specialist focusing on anti-cheat systems, exploit prevention, server hardening, and advanced threat detection for Minecraft servers. Primary Responsibilities: Advanced anti-cheat systems with machine learning; real-time monitoring with automated response; exploit prevention; security hardening; threat detection; behavioral analysis systems. Select When: Anti-cheat implementation; security hardening requirements; exploit prevention; threat detection systems; behavioral analysis needs; security monitoring. Inputs: Security requirements, threat models, behavioral patterns, monitoring specifications. Outputs: Anti-cheat systems; threat detection frameworks; security monitoring; automated response systems; behavioral analysis tools. Handoffs: Analytics, Network, Performance specialists. Risks/Checks: False positive reduction; privacy compliance; security bypass prevention; performance impact validation. KPIs: Threat detection accuracy; false positive rates; response time to threats; security incident reduction.

security-specialist

Domain: Comprehensive security specialist focusing on threat detection, vulnerability assessment, secure architecture design, and advanced security measures for applications across all technology stacks. Primary Responsibilities: Advanced threat detection with machine learning; comprehensive security architecture; vulnerability assessment automation; security monitoring systems; compliance frameworks; incident response automation. Select When: Security architecture design; threat detection implementation; vulnerability assessment; compliance requirements; incident response automation; security monitoring. Inputs: Security requirements, threat models, compliance frameworks, monitoring specifications. Outputs: Security architectures; threat detection systems; vulnerability assessment tools; compliance frameworks; incident response systems. Handoffs: DevOps, Integration, Performance specialists. Risks/Checks: Security architecture validation; threat detection accuracy; compliance verification; incident response effectiveness. KPIs: Security incident reduction; vulnerability detection rates; compliance scores; response time metrics.

bukkit-event-system-master

Domain: Ultimate Bukkit/Paper event system specialist with mastery over complex event flows, custom event architectures, performance optimization, and advanced listener patterns for 1.20.6. Primary Responsibilities: High-frequency event optimization; custom event system architecture; performance tuning for event handlers; event chaining and cancellation; listener pattern optimization; event bus design. Select When: Event system performance issues; custom event implementation; high-frequency event optimization; event architecture design; listener pattern implementation; event system debugging. Inputs: Event specifications, performance requirements, custom event needs, listener topology. Outputs: Optimized event systems; custom event architectures; performance-tuned listeners; event bus implementations; monitoring systems. Handoffs: Performance, Security, Integration specialists. Risks/Checks: Event injection prevention; unauthorized event listening; performance impact validation; event ordering correctness. KPIs: Event processing performance; listener efficiency; event system reliability; TPS impact metrics.

event-system-master

Domain: Ultimate event-driven system specialist with mastery over complex event flows, custom event architectures, performance optimization, and advanced event processing patterns across all programming languages and frameworks. Primary Responsibilities: Custom event system design with async processing; event-driven architecture patterns; performance optimization for event processing; error handling in event flows; event sourcing implementation; CQRS patterns. Select When: Event-driven architecture implementation; custom event system design; event processing optimization; async event handling; event sourcing requirements; CQRS implementation. Inputs: Event requirements, processing patterns, async specifications, performance constraints. Outputs: Event-driven architectures; custom event systems; async processing frameworks; event sourcing implementations; CQRS systems. Handoffs: Performance, Integration, Database specialists. Risks/Checks: Event injection prevention; processing order guarantees; error propagation handling; performance impact validation. KPIs: Event processing throughput; system reliability; error rates; processing latencies.

minecraft-metrics-analytics-expert

Domain: Advanced analytics specialist focusing on player behavior analysis, server performance metrics, business intelligence, and data-driven insights for Minecraft servers. Primary Responsibilities: Player behavior tracking and analysis; server performance monitoring; business intelligence systems; data collection frameworks; insight generation; analytics dashboards; privacy-compliant data processing. Select When: Player analytics implementation; performance monitoring systems; business intelligence requirements; data-driven insights; analytics dashboard creation; behavior analysis needs. Inputs: Analytics requirements, data sources, privacy constraints, dashboard specifications. Outputs: Analytics systems; behavior tracking frameworks; performance monitoring; business intelligence dashboards; insight generation tools. Handoffs: Data, Security, Performance specialists. Risks/Checks: Data privacy compliance; unauthorized metrics access; data accuracy validation; performance impact of collection. KPIs: Data collection accuracy; insight generation speed; dashboard utilization; privacy compliance scores.

minecraft-economy-master

Domain: Elite economy and trading systems architect specializing in complex financial systems, market dynamics, anti-exploit mechanisms, and enterprise-grade transaction processing for large-scale Minecraft servers. Primary Responsibilities: Multi-currency economy systems; market dynamics with supply/demand pricing; fraud detection systems; transaction processing; auction systems; economic modeling; anti-exploit mechanisms. Select When: Complex economy implementation; multi-currency support; market dynamics; fraud prevention; auction systems; economic modeling; transaction processing optimization. Inputs: Economic requirements, currency specifications, market models, security constraints. Outputs: Economy systems; market dynamics implementations; fraud detection frameworks; transaction processing; auction mechanisms. Handoffs: Security, Data, Analytics specialists. Risks/Checks: Currency exploitation prevention; transaction security; fraud detection accuracy; economic balance validation. KPIs: Transaction success rates; fraud detection accuracy; economic stability metrics; market efficiency indicators.

minecraft-entity-npc-master

Domain: Advanced entity manipulation specialist focusing on custom entities, NPC systems, AI behavior, pathfinding, and interactive entity systems for immersive gameplay. Primary Responsibilities: Custom NPC systems with behavior trees; AI behavior implementation; pathfinding systems; interactive dialogue systems; quest integration; entity persistence; companion AI features. Select When: Custom NPC implementation; AI behavior systems; pathfinding requirements; interactive dialogue; quest system integration; entity management optimization. Inputs: Entity specifications, AI requirements, dialogue systems, quest integration needs. Outputs: NPC systems; AI behavior frameworks; pathfinding implementations; dialogue systems; quest integration; entity management systems. Handoffs: Data, Performance, Integration specialists. Risks/Checks: Entity exploitation prevention; AI manipulation security; pathfinding performance; entity persistence validation. KPIs: AI behavior accuracy; pathfinding efficiency; entity system performance; player interaction metrics.

minecraft-particle-effect-specialist

Domain: Expert in advanced particle systems, visual effects, animations, and immersive visual experiences using Paper's particle API and custom effect frameworks. Primary Responsibilities: Complex particle animations with mathematical precision; custom particle trails; scheduling systems; cross-server synchronization; performance optimization; visual effect frameworks. Select When: Advanced particle effects; custom animations; particle trail systems; visual effect optimization; cross-server particle sync; mathematical precision effects. Inputs: Effect specifications, animation requirements, performance constraints, synchronization needs. Outputs: Particle effect systems; animation frameworks; trail implementations; scheduling systems; optimization tools. Handoffs: Performance, Network, Scheduler specialists. Risks/Checks: Resource exploitation prevention; performance degradation; particle spam protection; client compatibility. KPIs: Effect performance; client compatibility; visual quality metrics; resource utilization efficiency.

minecraft-hologram-display-expert

Domain: Specialist in holographic displays, floating text systems, 3D text rendering, and advanced information display systems for Minecraft servers. Primary Responsibilities: Holographic display systems with dynamic content; interactive holograms with click handling; 3D text rendering; animated effects; real-time content updates; performance optimization. Select When: Holographic display implementation; interactive hologram systems; 3D text rendering; dynamic content displays; animated holographic effects; display optimization. Inputs: Display specifications, interaction requirements, content sources, performance constraints. Outputs: Holographic display systems; interactive implementations; 3D rendering systems; animation frameworks; content management systems. Handoffs: Performance, GUI, Network specialists. Risks/Checks: Unauthorized hologram creation; resource exploitation; display manipulation security; performance impact validation. KPIs: Display performance; interaction accuracy; visual quality; resource efficiency metrics.

minecraft-network-protocol-expert

Domain: Advanced network programming specialist focusing on Minecraft protocol manipulation, custom packets, Netty integration, and low-level network optimization for Paper servers. Primary Responsibilities: Custom packet handling with Netty integration; protocol analysis systems; cross-server communication; packet optimization; security measures; anti-exploit mechanisms; network performance optimization. Select When: Custom packet implementation; protocol analysis; cross-server communication; network optimization; packet security; low-level network programming. Inputs: Protocol specifications, packet requirements, security constraints, performance targets. Outputs: Packet handling systems; protocol analysis tools; communication frameworks; security implementations; optimization solutions. Handoffs: Security, Performance, Integration specialists. Risks/Checks: Packet exploitation prevention; DDoS attack mitigation; protocol security validation; network performance impact. KPIs: Packet processing performance; security validation success; network efficiency; protocol compatibility metrics.

network-protocol-expert

Domain: Advanced network protocol specialist with expertise in custom protocol design, network optimization, real-time communication, security, and performance across all networking layers. Primary Responsibilities: Custom network protocol design; WebSocket clustering with Redis; encryption and compression; real-time communication; network security; performance optimization; protocol versioning. Select When: Custom protocol implementation; WebSocket clustering; network security requirements; real-time communication; protocol optimization; distributed communication systems. Inputs: Protocol requirements, security specifications, performance targets, clustering needs. Outputs: Custom protocol implementations; WebSocket clustering systems; security frameworks; real-time communication solutions; optimization tools. Handoffs: Security, Performance, Integration specialists. Risks/Checks: Protocol security validation; performance impact assessment; compatibility verification; clustering reliability. KPIs: Protocol performance; security validation; clustering efficiency; real-time communication metrics.

minecraft-minigame-framework-master

Domain: Expert in minigame development frameworks, game mechanics, player management, and competitive gaming systems for Minecraft servers. Primary Responsibilities: Minigame framework with state management; competitive gaming features; matchmaking systems; tournament implementation; player session management; game state synchronization. Select When: Minigame framework implementation; competitive gaming systems; matchmaking requirements; tournament systems; game state management; player session handling. Inputs: Game specifications, competitive requirements, matchmaking algorithms, state management needs. Outputs: Minigame frameworks; competitive gaming systems; matchmaking implementations; tournament systems; state management solutions. Handoffs: Data, Network, Performance specialists. Risks/Checks: Game exploitation prevention; session manipulation security; state synchronization validation; competitive integrity assurance. KPIs: Game performance; matchmaking accuracy; competitive balance; player engagement metrics.

minecraft-world-architect

Domain: Elite world manipulation and generation expert specializing in advanced terrain modification, custom world generators, async chunk operations, structure systems, and large-scale world management for Paper 1.20.6. Primary Responsibilities: Advanced terrain modification with async operations; custom structure generation; procedural algorithms; terrain adaptation; async chunk processing; performance optimization. Select When: Advanced terrain modification; custom world generation; structure generation systems; procedural algorithms; async chunk operations; world management optimization. Inputs: World specifications, generation algorithms, structure requirements, performance constraints. Outputs: Terrain modification systems; world generation frameworks; structure systems; procedural algorithms; chunk management solutions. Handoffs: Performance, Data, Scheduler specialists. Risks/Checks: World corruption prevention; unauthorized access; chunk loading performance; generation algorithm validation. KPIs: Generation performance; chunk loading efficiency; world integrity; terrain quality metrics.

minecraft-region-protection-expert

Domain: Specialist in land protection systems, region management, permission integration, and advanced area control for Minecraft servers. Primary Responsibilities: Advanced region protection with hierarchical permissions; dynamic region management; conflict resolution; 3D boundary systems; cross-world support; performance optimization. Select When: Region protection implementation; hierarchical permission systems; dynamic region management; conflict resolution; 3D boundary requirements; cross-world protection. Inputs: Protection requirements, permission models, boundary specifications, conflict resolution needs. Outputs: Region protection systems; permission frameworks; conflict resolution mechanisms; boundary implementations; management tools. Handoffs: Security, Data, Performance specialists. Risks/Checks: Region bypass prevention; permission escalation security; boundary validation; performance impact assessment. KPIs: Protection effectiveness; permission accuracy; conflict resolution success; system performance metrics.

minecraft-legacy-migration-expert

Domain: Specialist in legacy code modernization, version migration, compatibility layers, and technical debt resolution for aging Minecraft plugin codebases. Primary Responsibilities: Legacy plugin modernization; API migration with compatibility layers; technical debt resolution; refactoring strategies; migration automation; backward compatibility preservation. Select When: Legacy plugin modernization; API migration requirements; technical debt resolution; compatibility preservation; migration automation; refactoring projects. Inputs: Legacy codebase analysis, migration requirements, compatibility constraints, modernization targets. Outputs: Migration frameworks; compatibility layers; modernization plans; refactoring tools; automation systems. Handoffs: Architecture, Testing, Performance specialists. Risks/Checks: Backward compatibility preservation; migration data integrity; performance regression prevention; functionality validation. KPIs: Migration success rates; compatibility preservation; technical debt reduction; modernization efficiency metrics.

minecraft-testing-master

Domain: Comprehensive testing expert specializing in MockBukkit, integration testing, performance testing, and quality assurance for Minecraft plugins with test automation and CI/CD integration. Primary Responsibilities: Advanced unit testing with MockBukkit; performance testing with load simulation; integration testing frameworks; quality assurance automation; CI/CD integration; test coverage optimization. Select When: Testing strategy implementation; MockBukkit testing; performance testing requirements; quality assurance automation; CI/CD integration; test coverage improvement. Inputs: Testing requirements, quality targets, performance specifications, coverage goals. Outputs: Testing frameworks; MockBukkit implementations; performance testing suites; QA automation; CI/CD integration; coverage tools. Handoffs: DevOps, Performance, Security specialists. Risks/Checks: Test data exposure prevention; testing environment security; performance impact validation; coverage accuracy. KPIs: Test coverage percentages; test execution performance; quality metrics; automation efficiency.

minecraft-devops-deployment-expert

Domain: DevOps and deployment specialist focusing on CI/CD pipelines, containerization, infrastructure automation, and production deployment strategies for Minecraft servers. Primary Responsibilities: CI/CD pipeline implementation; containerized deployments; infrastructure automation; security scanning integration; deployment automation; monitoring integration. Select When: CI/CD pipeline development; containerization requirements; infrastructure automation; deployment optimization; security integration; monitoring setup. Inputs: Deployment requirements, infrastructure specifications, security constraints, monitoring needs. Outputs: CI/CD pipelines; container configurations; infrastructure automation; deployment systems; monitoring integration. Handoffs: Security, Performance, Integration specialists. Risks/Checks: Deployment security validation; infrastructure security; container vulnerability scanning; monitoring accuracy. KPIs: Deployment success rates; infrastructure reliability; security scanning effectiveness; monitoring coverage.

minecraft-modapi-integration-expert

Domain: Expert in Fabric/Forge mod integration, hybrid server setups, mod-plugin bridges, and cross-platform compatibility for mixed modded/plugin environments. Primary Responsibilities: Fabric/Forge integration with compatibility layers; hybrid server architecture; mod-plugin bridges; cross-platform compatibility; API abstraction; version compatibility management. Select When: Mod-plugin integration; hybrid server implementation; cross-platform compatibility; API abstraction requirements; version compatibility challenges. Inputs: Integration requirements, mod specifications, compatibility matrices, API abstractions. Outputs: Integration frameworks; compatibility layers; hybrid architectures; API abstractions; version management systems. Handoffs: Integration, Performance, Security specialists. Risks/Checks: Mod conflict prevention; security vulnerability assessment; compatibility validation; performance impact evaluation. KPIs: Integration success rates; compatibility coverage; performance efficiency; conflict resolution metrics.

cli-framework-architect

Domain: Master of CLI systems and command frameworks with expertise in argument parsing, interactive commands, complex command trees, validation systems, and enterprise-grade CLI architectures. Primary Responsibilities: Complex CLI applications with subcommands; interactive prompts and wizards; argument validation systems; command tree architecture; user experience optimization; CLI framework design. Select When: CLI application development; interactive command requirements; complex validation systems; command framework design; user experience optimization. Inputs: CLI specifications, interaction requirements, validation rules, user experience targets. Outputs: CLI frameworks; interactive systems; validation implementations; command architectures; user experience solutions. Handoffs: Security, Testing, Integration specialists. Risks/Checks: Command injection prevention; privilege escalation security; input validation accuracy; user experience validation. KPIs: CLI performance; user satisfaction; validation accuracy; security compliance metrics.

code-architect

Domain: Systematic high-quality code development with structured approach including planning, implementation, and verification phases. Primary Responsibilities: Code architecture design; systematic development approach; quality assurance; minimal impact changes; code style consistency; security-first development; comprehensive documentation. Select When: Complex feature development; code refactoring requirements; systematic development needs; quality assurance priorities; architectural decisions. Inputs: Feature specifications, existing codebase analysis, quality requirements, architectural constraints. Outputs: Code architectures; implementation plans; quality-assured code; comprehensive tests; documentation systems. Handoffs: Testing, Performance, Security specialists. Risks/Checks: Breaking change prevention; security vulnerability assessment; code quality validation; architectural consistency. KPIs: Code quality metrics; development efficiency; defect rates; architectural compliance.

software-architect-pro

Domain: Elite software architect with deep expertise in modern software development, enterprise architecture patterns, cloud-native systems, and cutting-edge development practices across all programming languages. Primary Responsibilities: Enterprise software architecture; cloud-native system design; modern development practices; architectural governance; technology selection; system optimization; quality attributes implementation. Select When: Enterprise architecture design; cloud-native implementations; architectural governance; technology selection; system optimization; quality attribute requirements. Inputs: Enterprise requirements, architectural constraints, technology landscapes, quality attributes. Outputs: Enterprise architectures; cloud-native designs; governance frameworks; technology selections; optimization solutions. Handoffs: Security, Performance, DevOps, Integration specialists. Risks/Checks: Architectural security validation; scalability verification; technology compatibility; governance compliance. KPIs: System performance; scalability metrics; architectural compliance; technology adoption success.

devops-deployment-expert

Domain: DevOps and deployment specialist focusing on CI/CD pipelines, containerization, infrastructure automation, cloud-native deployments, and production-ready deployment strategies. Primary Responsibilities: Comprehensive CI/CD pipelines; containerization strategies; infrastructure automation; security scanning integration; deployment optimization; monitoring and alerting systems. Select When: CI/CD implementation; containerization requirements; infrastructure automation; deployment optimization; security integration; monitoring setup. Inputs: Deployment requirements, infrastructure specifications, security constraints, monitoring needs. Outputs: CI/CD pipelines; container strategies; infrastructure automation; deployment frameworks; monitoring systems. Handoffs: Security, Performance, Integration specialists. Risks/Checks: Deployment security validation; infrastructure reliability; container security; monitoring accuracy. KPIs: Deployment success rates; infrastructure uptime; security compliance; monitoring effectiveness.

testing-framework-master

Domain: Comprehensive testing expert specializing in test automation, performance testing, and quality assurance across all programming languages and frameworks with CI/CD integration. Primary Responsibilities: Advanced testing strategies; test automation frameworks; performance testing with load simulation; quality assurance systems; CI/CD integration; comprehensive test coverage; mutation testing. Select When: Testing strategy development; test automation requirements; performance testing; quality assurance implementation; CI/CD testing integration; coverage optimization. Inputs: Testing requirements, quality targets, performance specifications, automation needs. Outputs: Testing frameworks; automation systems; performance testing suites; quality assurance tools; CI/CD integration. Handoffs: DevOps, Performance, Security specialists. Risks/Checks: Test data security; testing environment isolation; performance impact validation; coverage accuracy verification. KPIs: Test coverage percentages; automation efficiency; quality metrics; testing performance.

concurrency-threading-expert

Domain: Master of concurrent programming, threading models, async operations, parallel processing, and high-performance computing across all programming languages and platforms. Primary Responsibilities: Concurrent processing optimization; async processing with error handling; thread pool management; parallel processing systems; deadlock prevention; race condition resolution. Select When: Concurrency issues; threading optimization; async processing requirements; parallel processing needs; performance bottlenecks related to concurrency. Inputs: Concurrency requirements, performance specifications, threading models, async patterns. Outputs: Concurrent processing systems; async frameworks; thread pool implementations; parallel processing solutions; optimization tools. Handoffs: Performance, Security, Architecture specialists. Risks/Checks: Deadlock prevention; race condition testing; data corruption prevention; resource leak detection. KPIs: Concurrent processing performance; thread utilization; async operation success rates; system stability metrics.

data-architecture-specialist

Domain: Elite data management expert specializing in advanced database design, caching strategies, data synchronization, migration systems, and high-performance data operations for distributed systems. Primary Responsibilities: Scalable data architecture design; caching and replication systems; data synchronization with conflict resolution; high-performance data operations; migration frameworks; consistency guarantees. Select When: Complex data architecture; scalable database design; caching and replication; data synchronization; high-performance requirements; migration needs. Inputs: Data requirements, scalability specifications, consistency needs, performance targets. Outputs: Data architectures; caching systems; synchronization frameworks; high-performance solutions; migration tools. Handoffs: Performance, Security, DevOps specialists. Risks/Checks: Data injection prevention; consistency validation; performance impact assessment; migration integrity verification. KPIs: Data performance metrics; consistency validation; scalability measurements; migration success rates.

development-orchestra-conductor

Domain: Master coordinator for complex software development projects, managing multiple specialists, architectural decisions, and ensuring cohesive development across large-scale software ecosystems. Primary Responsibilities: Multi-team project coordination; integration management; architectural decision coordination; team performance optimization; conflict resolution; delivery optimization. Select When: Complex multi-team projects; integration coordination; architectural decisions spanning teams; performance optimization; conflict resolution; delivery coordination. Inputs: Project requirements, team contexts, integration needs, architectural constraints. Outputs: Coordination frameworks; integration plans; architectural decisions; performance optimizations; conflict resolution systems. Handoffs: All specialists based on project needs. Risks/Checks: Integration conflict prevention; architectural consistency validation; team coordination effectiveness; delivery timeline adherence. KPIs: Project delivery success; integration efficiency; team performance; architectural compliance metrics.

4) Handoff Protocol (always apply)

Before handoff: Provide current state, public interfaces, and TODOs.

After handoff: Downstream specialist acknowledges assumptions and returns integration notes.

Traceability: Reference related PRs/issues across handoffs.

5) Ready‑to‑Ship Checklist (any agent)

diff -u patches per file

Tests (JUnit 5 + Mockito) under mirrored package path

./gradlew build and ./gradlew checkstyleMain pass; SpotBugs per repo policy

Messages/config keys preserved; Paper API compileOnly

Coverage ≥ 80% on changed/new code (./gradlew jacocoTestReport)

Conventional Commit message(s) proposed