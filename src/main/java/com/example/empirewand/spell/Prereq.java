package com.example.empirewand.spell;

import net.kyori.adventure.text.Component;

public record Prereq(boolean canCast, Component reason) {
}