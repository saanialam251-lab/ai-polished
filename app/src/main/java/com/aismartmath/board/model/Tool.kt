package com.aismartmath.board.model

/** One board tool. Text copied from the web version's TL table. */
data class Tool(val id: String, val icon: String, val name: String, val hint: String)

object Tools {
    val byId: Map<String, Tool> = listOf(
        Tool("pt", "•", "Point", "Tap to place a point"),
        Tool("seg", "╱", "Line", "Drag to draw a line"),
        Tool("tri", "△", "Triangle", "Tap 3 points to make a triangle"),
        Tool("rtri", "◺", "Right △", "Drag to draw a right triangle"),
        Tool("quad", "▱", "Quadrilateral", "Tap 4 points"),
        Tool("rect", "▭", "Rectangle", "Drag from corner to corner"),
        Tool("pent", "⬠", "Pentagon", "Tap 5 points"),
        Tool("hex", "⬡", "Hexagon", "Tap 6 points"),
        Tool("circ", "○", "Circle", "Drag from the centre outwards"),
        Tool("unit", "◔", "Unit circle", "Drag to change the angle"),
        Tool("fx", "ƒ", "Graph", "Type an equation, then Plot"),
        Tool("trig", "📐", "Triangle", "Use the sliders"),
        Tool("pan", "✋", "Move", "Drag to move · pinch or ＋/－ to zoom"),
        Tool("sel", "👆", "Select", "Tap: edit · 2: copy/cut · 3: move · 4: resize · 5: bisect"),
        Tool("sk", "⬚", "Content", "Drag a box round things to select them — or sketch a shape"),
        Tool("q", "🤖", "Ask AI", "Type any maths, physics, chemistry or English question — AI solves it step by step"),
        Tool("curve", "〰", "Curve", "Draw a curved line with the pen"),
        Tool("cube", "🧊", "Cube", "Tap to place a cube"),
        Tool("cuboid", "📦", "Cuboid", "Tap to place a cuboid"),
        Tool("pyr", "🔺", "Pyramid", "Tap to place a pyramid"),
        Tool("prism", "⛺", "Prism", "Tap to place a prism"),
        Tool("cyl", "🥫", "Cylinder", "Tap to place a cylinder"),
        Tool("cone", "🍦", "Cone", "Tap to place a cone"),
        Tool("sph", "🔮", "Sphere", "Tap to place a sphere"),
        Tool("dv", "𝑓′", "Derivative", "Type f(x), then tap ƒ′ to add its derivative"),
        Tool("pen", "🖊️", "Pen", "Write with your finger — pick colours below"),
        Tool("hl", "🖍️", "Highlight", "Highlight over your work"),
        Tool("er", "🧽", "Eraser", "Drag over strokes to erase them"),
    ).associateBy { it.id }
}
