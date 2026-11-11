package com.aismartmath.board.model

import androidx.compose.ui.graphics.Color

/** One home-screen topic. Copied from the web version's MODS table (same order, icons, colours, tools). */
data class Topic(
    val id: String,
    val icon: String,
    val name: String,
    val color: Color,
    val toolIds: List<String>,
    val axes: Boolean,
)

/** Every tool is available in every topic: writing, shapes, solids, graphs, answers and content selection. */
private val EVERY = listOf(
    "pen", "hl", "er", "sk", "q", "pt", "seg", "tri", "rtri", "quad", "rect", "pent", "hex", "circ",
    "fx", "dv", "trig", "unit", "cube", "cuboid", "pyr", "prism", "cyl", "cone", "sph", "sel", "pan",
)

object Topics {
    val all: List<Topic> = listOf(
        Topic("tri", "△", "Triangles", Color(0xFF9C6644), listOf("tri", "rtri", "sk", "seg", "pt", "sel", "pan"), false),
        Topic("quad", "▱", "Quadrilaterals", Color(0xFFB5651D), listOf("rect", "quad", "pent", "hex", "sk", "seg", "pt", "sel", "pan"), false),
        Topic("circ", "○", "Circles", Color(0xFF8B6F47), listOf("circ", "sk", "seg", "pt", "sel", "pan"), false),
        Topic("coord", "⌖", "Coordinate Geometry", Color(0xFFC68B2C), listOf("fx", "dv", "sk", "pt", "seg", "tri", "rect", "quad", "circ", "sel", "pan"), true),
        Topic("der", "𝑓′", "Derivatives", Color(0xFFA8452F), listOf("dv", "pt", "seg", "sel", "pan"), true),
        Topic("solve", "✍️", "Solve", Color(0xFFB8962E), listOf("pen", "hl", "er", "sk", "sel", "seg", "pan"), false),
        Topic("trig", "∠", "Plane Trigonometry", Color(0xFF9A6B52), listOf("trig", "unit", "rtri", "sel", "pan"), true),
        Topic("line", "╱", "Lines & Points", Color(0xFF7D6B57), listOf("seg", "pt", "sel", "pan"), true),
        Topic("par", "∥", "Parallel Lines", Color(0xFFC4682B), listOf("seg", "pt", "sel", "pan"), true),
        Topic("solid", "🧊", "3D Solids", Color(0xFF8A7155), listOf("cube", "cuboid", "pyr", "prism", "cyl", "cone", "sph", "sel", "pan"), false),
        Topic("all", "✳", "All Together", Color(0xFF7A6232), listOf("sk", "pt", "seg", "tri", "rtri", "quad", "pent", "hex", "rect", "circ", "fx", "dv", "trig", "unit", "cube", "cuboid", "pyr", "prism", "cyl", "cone", "sph", "sel", "pan"), true),
    ).map { it.copy(toolIds = (it.toolIds + EVERY).distinct()) }
    val byId: Map<String, Topic> = all.associateBy { it.id }
}
