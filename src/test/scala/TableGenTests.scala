package wavegen

import chisel3._
import chiseltest._
import chisel3.experimental.BundleLiterals._
import org.scalatest.flatspec.AnyFlatSpec


class TableGenTests extends AnyFlatSpec with ChiselScalatestTester {
	object Tester {
		def apply[G <: Generator](gen: G, period: Int, resolution: Int)(block: (TableGen[G], Int => Unit) => Any) = {
			test(new TableGen(gen, period, resolution)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
				def step(): Unit = { dut.clock.step() }
				def expect(value: Int): Unit = { dut.io.out.expect(value.U); step() }
				block(dut, expect)
			}
		}
	}

	behavior of "TableGen"
	for (period <- 2 to 10 by 2) {
		for (resolution <- 2 to 10) {
			it should s"generate squares(p=$period, r=$resolution)" in {
				Tester(new SquareGenerator(), period, resolution) { (dut, expect) => 
					for (i <- 1 to 10) {
						for (j <- 0 until period by 2)
							expect(resolution)

						for (j <- 0 until period by 2)
							expect(0)
					}
				}
			}
		}
	}
}
