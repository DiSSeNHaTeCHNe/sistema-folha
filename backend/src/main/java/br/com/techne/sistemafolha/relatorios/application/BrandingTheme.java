package br.com.techne.sistemafolha.relatorios.application;

import java.util.Optional;

public record BrandingTheme(
    String primaryColor,
    String secondaryColor,
    String textColor,
    String mutedBackground,
    Optional<byte[]> logoBytes
) {}
