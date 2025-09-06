package com.example.empirewand.spell;

import net.kyori.adventure.text.Component;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(value = {
        "EI_EXPOSE_REP" }, justification = "Component is immutable from our usage perspective (Adventure API); record provides direct access.")
public record Prereq(boolean canCast, Component reason) {
}