package wavegen

trait Generator {
	def apply(time: Double): Double
}

class SineGenerator extends Generator {
	override def apply(time: Double): Double = {
		(1 + Math.sin(time * Math.PI * 2)) / 2
	}
}

class SawtoothGenerator(rising: Boolean) extends Generator {
	override def apply(time: Double): Double = {
		if (rising) {
			time % 1.0
		} else {
			1.0 - (time % 1.0)
		}
	}
}

class TriangleGenerator(startHigh: Boolean) extends Generator {
	override def apply(time: Double): Double = {
		val modulo = time % 1.0
		val base = if (modulo < 0.5) 2 * modulo else 2 * (1 - modulo)
		if (startHigh)
			1 - base
		else
			base
	}
}

class RectangularGenerator(dutyCycle: Double) extends Generator {
	override def apply(time: Double): Double = {
		if ((time % 1.0) < dutyCycle)
			1.0
		else
			0.0
	}
}

class SquareGenerator extends RectangularGenerator(0.5)
