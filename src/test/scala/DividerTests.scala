package wavegen

import chisel3._
import chisel3.experimental._
import chisel3.util._
import chiseltest._
import chisel3.experimental.BundleLiterals._
import org.scalatest.flatspec.AnyFlatSpec

class DividerTests extends AnyFlatSpec with ChiselScalatestTester {
	behavior of "Divider"
	it should "divide properly" in {
		val width = 3

		test(new Divider(width)) { dut =>
			var maxSteps = 0
			var totalSteps: Long = 0
			var trials = 0

			dut.clock.setTimeout(0)

			for (den <- 1 to ((1 << width) - 1)) {
				for (num <- 0 to ((1 << width) - 1)) {
					trials += 1
					dut.io.in.bits.numerator.poke(num)
					dut.io.in.bits.denominator.poke(den)
					dut.io.in.valid.poke(true)
					dut.clock.step()
					dut.io.in.valid.poke(false)
					var steps = 1
					while (dut.io.out.valid.peek().litValue.toInt == 0) {
						dut.clock.step()
						steps += 1
					}
					if (maxSteps < steps)
						maxSteps = steps
					totalSteps += steps
					val failed = (dut.io.out.bits.quotient.peek().litValue.toInt != num / den) || (dut.io.out.bits.remainder.peek().litValue.toInt != num % den)
					if (failed) {
						println(f"$num, $den: expecting quo = ${num / den}, rem = ${num % den}")
						println(f"     -> $steps steps: quo = ${dut.io.out.bits.quotient.peek().litValue.toInt}, rem = ${dut.io.out.bits.remainder.peek().litValue.toInt}")
						println("\u001b[31mFAILURE\u001b[39m")
						println()
					}
					dut.io.out.bits.quotient.expect(num / den)
					dut.io.out.bits.remainder.expect(num % den)
				}

				println(f"Finished denominator $den")
			}

			println(f"Maximum steps: $maxSteps")
			if (0 < trials) {
				println(f"Average steps: ${totalSteps.toDouble / trials}")
			}
		}
	}
}
