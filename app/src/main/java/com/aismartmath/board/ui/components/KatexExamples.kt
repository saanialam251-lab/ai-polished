package com.aismartmath.board.ui.components

/** One group of examples on the KaTeX page: what it is, and things to type (each is drawn live next to the typing). */
class Cat(val title: String, val note: String, val items: List<String>)

val KATEX_CATS: List<Cat> = listOf(
    Cat(
        "1  Powers and small numbers", "Type ^ for a power and _ for a small number underneath. Use ( ) or { } when it is more than one character.",
        listOf("x^2", "x^3", "a^x", "x_1", "x_n", "x_1^2", "x^(n+1)", "x^{2+3}", "e^(-x^2/2)", "2^10", "a^(-1)", "x^-2"),
    ),
    Cat(
        "2  Towers (a power of a power)", "Both ways work: a^x^2 and a^(x^2) mean a to the power (x to the power 2). Go as deep as you like.",
        listOf("a^x^2", "a^(x^y)", "a^(x^(y^z))", "2^(3^4)", "a^(b^(c^(d^e)))", "a_1^(x^y)", "(a^x)^2", "e^(e^x)", "x^(2^n)", "pi^(2^3)"),
    ),
    Cat(
        "3  Fractions", "Just write a/b. Numbers, letters, powers and brackets next to the slash belong to the fraction. You can also write a upon b.",
        listOf("a/b", "1/2", "x/y + z/w", "(a+b)/c", "a/(b+c)", "(a+b)/(c+d)", "2x/3y", "x^2/y^2", "1/x", "a upon b", "x^2/(x+1)", "(x+1)/(x-1)"),
    ),
    Cat(
        "4  Fraction upon fraction", "Put a fraction inside the top or the bottom with brackets — as many levels as you want.",
        listOf("a/(b/c)", "(a/b)/c", "(a/b)/(c/d)", "((a/b)/(c/d))/(e/f)", "1/(1+1/x)", "1/(1+1/(1+1/x))", "(1/2)/(3/4)", "(x/y)/(z/w)"),
    ),
    Cat(
        "5  Roots", "sqrt( ), sqr( ) or root( ) for a square root, cbrt( ) for a cube root, 4rt( ) for a fourth root. The bar grows over whatever is inside.",
        listOf("sqrt(x)", "sqrt x", "sqr(x)", "sqr(x+1)", "root(x)", "root(a/b)", "cbrt(x)", "cbrt(a/b)", "4rt(x)", "4rt(a/b)", "sqrt(x^2+y^2)", "sqrt(a^x/b^y)", "sqrt(2)/2", "root(2pi/(3pi/2))"),
    ),
    Cat(
        "5b  Any root: fourth, fifth, fractional, infinite …", "Write the index, then brt, then what is under the root: 4brt6 gives a fourth root of 6. The index can be a number, a letter, inf, or anything in brackets such as (4/5). A big index is drawn smaller; if it still does not fit, the root sign grows.",
        listOf(
            "sqrt4", "cbrt4", "4brt6", "5brt(a/b)", "(4/5)brt(6/8)", "(1/2)brt(x^2+1)", "nbrt(x)", "infbrt(x)", "alphabrt(x)", "(a/b)brt(c/d)",
            "(int_0^1 x dx)brt(6)", "(lim_(x->0) sin(x)/x)brt(a/b)", "(sum_(i=1)^n i)brt(x)", "(a^(x^y)/b^(p^q))brt((c/d)/(e/f))", "4rt(x)",
        ),
    ),
    Cat(
        "6  Roots together and inside each other", "Roots can be added, nested, put over each other or hold towers and fractions of fractions.",
        listOf(
            "sqrt(a) + sqrt(b)", "sqrt(a) + sqrt(b) + sqrt(c^(a^(z^z)))", "sqrt(a) + sqrt(b) - sqrt(c)", "root(cbrt(a/b))", "cbrt(root(a/b))",
            "(root(a/b))/(cbrt(c/d))", "root(cbrt((a/b)/(c/d)))", "sqrt(a+sqrt(b+sqrt(c)))", "root((a^x/b^y)/(c^z/d^w))", "sqrt(1+x^2)/x",
        ),
    ),
    Cat(
        "7  Brackets", "Brackets grow to fit what is inside. In (a+b)/(c+d) the outer brackets are dropped because the fraction bar already groups them.",
        listOf("(a+b)", "(a/b)", "[a/b]", "{a, b, c}", "|x - 3|", "(a/b)^x", "(a/b)^(x/y)", "((a/b))", "(a/b) + (c/d)", "2(a/b)", "[x^2/y^2]"),
    ),
    Cat(
        "8  Greek letters", "Write the name: alpha, beta, pi, theta … Capital names (Gamma, Delta, Sigma, Omega) give capital letters.",
        listOf(
            "alpha beta gamma delta", "epsilon zeta eta theta", "lambda mu nu xi", "pi rho sigma tau", "phi chi psi omega", "Gamma Delta Theta Lambda", "Sigma Phi Psi Omega",
            "alpha/beta", "lambda/omega", "phi/psi", "alpha^(beta^gamma)", "pi^(2^3)", "2pi", "root(alpha/beta)", "(alpha^2/beta^2)/(gamma^2/delta^2)", "Gamma(z)", "root(Gamma(a/b))",
        ),
    ),
    Cat(
        "9  Limits", "lim_(…) puts what x is heading to underneath. Write -> for the arrow and inf for infinity.",
        listOf(
            "lim_(x->0) a/b", "lim_(x->0) sin(x)/x", "lim_(x->inf) 1/x", "lim_(x->inf) a^(x^y)/b^x", "lim_(n->inf) (1+1/n)^n", "lim_(x->0) root(a/b)",
            "lim_(a/b -> c/d) root(a/b)", "lim_(x->0) (a^x/b^y)/(c/d)", "lim_(x->3) (x^2-9)/(x-3)", "\\lim_{x \\to 0} \\frac{a}{b}",
        ),
    ),
    Cat(
        "10  Sums and products", "sum_(start)^end and prod_(start)^end. What comes after is the thing being added or multiplied.",
        listOf("sum_(i=1)^n i", "sum_(i=1)^n a_i", "sum_(i=1)^n a_i/b_i", "sum_(n=1)^inf 1/n^2", "sum_(k=0)^n k^2/n", "sum_(i=1)^n root(a_i/b_i)", "prod_(i=1)^n a_i", "prod_(i=1)^n a_i/b_i"),
    ),
    Cat(
        "11  Integrals", "int for the integral sign; int_(from)^(to) for the limits. dx is just d and x.",
        listOf(
            "int a dx", "int sin(x)/x dx", "int x^2 dx", "int_(0)^1 a/b dx", "int_a^b f(x) dx", "int_(0)^inf root(a/b) dx", "int_(0)^(pi/2) sin(x) dx",
            "int_(a^(x^y))^(b^(p^q)) c dx", "int root(a/b) dx",
        ),
    ),
    Cat(
        "12  Logs, trig and exp", "Function names are upright; log_(a) b gives a small base. Put a power right after the name: sin^2(x).",
        listOf(
            "log_(a) b", "log_(a/b) (c/d)", "(log_(a) b)/(log_(c) d)", "ln(x)", "exp(a^(x^y))", "sin(a/b)", "root(sin(a/b)/cos(c/d))",
            "sin^2(x) + cos^2(x) = 1", "tan(x) = sin(x)/cos(x)", "sec(x) csc(x) cot(x)",
        ),
    ),
    Cat(
        "13  Derivatives", "d/dx and dy/dx are just fractions. d2y/dx2 gives the second derivative.",
        listOf("d/dx (a/b)", "d/dx root(a/b)", "d/dx (a^2/b^2)", "dy/dx", "d2y/dx2", "f'(x)", "f''(x)"),
    ),
    Cat(
        "14  Words that become symbols", "Short words are turned into the symbol: deg, angle, triangle, perp, parallel, cong, vec, hat, bar, floor, ceil, binom.",
        listOf(
            "vec a / vec b", "hat a / bar b", "floor(a/b)", "ceil(a/b)", "binom n k", "30deg", "angle ABC", "triangle ABC", "arc AB", "AB perp CD", "AB parallel CD",
            "triangle ABC cong triangle DEF", "angle ABC = 30deg",
        ),
    ),
    Cat(
        "15  Operators and relations", "Typed arrows and comparisons become the real signs.",
        listOf("a <= b", "a >= b", "a != b", "a +- b", "a * b", "2 * 3", "a => b", "a <=> b", "a -> b", "x \\in A \\cup B", "A \\subset B", "x \\approx y", "a \\equiv b", "a \\propto b", "a \\times b \\div c"),
    ),
    Cat(
        "16  LaTeX works too", "If you know LaTeX / KaTeX you can use it as it is — mix it with the short forms in the same text.",
        listOf(
            "\\frac{a}{b}", "\\dfrac{a}{b}", "\\tfrac{a}{b}", "\\sqrt[3]{x}", "\\sqrt{x^2+y^2}", "\\left( \\frac{a}{b} \\right)", "\\frac{\\frac{a}{b}}{\\frac{c}{d}}",
            "\\sum_{i=1}^{n} i", "\\prod_{i=1}^{n} i", "\\int_{a}^{b} x dx", "\\lim_{x \\to 0} sin(x)/x", "\\binom{n}{k}",
        ),
    ),
    Cat(
        "17  Matrices and cases", "\\begin{pmatrix} … \\end{pmatrix}: & between columns, \\\\ between rows.",
        listOf(
            "\\begin{matrix} a & b \\\\ c & d \\end{matrix}", "\\begin{pmatrix} a & b \\\\ c & d \\end{pmatrix}", "\\begin{bmatrix} a & b \\\\ c & d \\end{bmatrix}",
            "\\begin{vmatrix} a & b \\\\ c & d \\end{vmatrix}", "\\begin{pmatrix} 1 & 0 & 0 \\\\ 0 & 1 & 0 \\\\ 0 & 0 & 1 \\end{pmatrix}",
            "\\begin{cases} x & x > 0 \\\\ -x & x <= 0 \\end{cases}",
        ),
    ),
    Cat(
        "18  Text and style", "Words inside a formula, bold, double-struck sets, and the little marks over a letter.",
        listOf(
            "\\text{hello} x", "\\mathbf{x}", "\\mathbb{R}", "\\mathbb{N} \\subset \\mathbb{Z}", "\\hat{x} \\bar{x} \\vec{x} \\dot{x} \\ddot{x} \\tilde{x}",
            "\\overline{AB}", "\\underline{x}", "\\widehat{ABC}",
        ),
    ),
    Cat(
        "19  Special symbols", "A few more you may need.",
        listOf(
            "\\infty", "\\partial f / \\partial x", "\\nabla f", "\\forall x \\exists y", "\\therefore x = 2", "\\angle ABC", "\\triangle ABC", "90^\\circ", "1 + 2 + \\cdots + n",
            "x \\rightarrow y", "a \\Rightarrow b", "A \\cap B = \\emptyset",
        ),
    ),
    Cat(
        "20  The big ones (stress tests)", "Everything at once: towers inside fractions inside roots inside integrals.",
        listOf(
            "(a^(x^y)/b^(p^q))/(c^(r^s)/d^(t^u))", "root((a^(x^y)/b^(p^q))/(c^(r^s)/d^(t^u)))", "cbrt((a^(x^y)/b^(p^q))/(c^(r^s)/d^(t^u)))",
            "int root((a^(x^y)/b^(p^q))/(c^(r^s)/d^(t^u))) dx", "lim_(x->0) (a^(x^y)/b^(p^q))/(c^(r^s)/d^(t^u))", "(lambda^(x^y)/omega^(p^q))/(phi/psi)",
            "sum_(i=1)^n root((a_i/b_i)/(c_i/d_i))", "(pi^(e^3)/e^(pi^3))/(root(2)/cbrt(3))", "a^(b^(c^(d^e)))", "root(a^(b^(c^d)))", "5^(3x)/x^(6x)", "root(5^(3x)/x^(6x))",
        ),
    ),
    Cat(
        "21  Famous formulas", "Real formulas, typed the quick way.",
        listOf(
            "x = (-b +- sqrt(b^2 - 4ac))/(2a)", "a^2 + b^2 = c^2", "E = m c^2", "A = pi r^2", "V = 4/3 pi r^3", "(a+b)^2 = a^2 + 2ab + b^2", "y = m x + c",
            "d = sqrt((x_2 - x_1)^2 + (y_2 - y_1)^2)", "n!/(k!(n-k)!)", "F = (G m_1 m_2)/r^2", "s = (a+b+c)/2", "sin(x)/x",
        ),
    ),
)
