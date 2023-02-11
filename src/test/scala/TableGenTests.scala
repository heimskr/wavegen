package wavegen

import chisel3._
import chiseltest._
import chisel3.experimental.BundleLiterals._
import org.scalatest.flatspec.AnyFlatSpec


class TableGenTests extends AnyFlatSpec with ChiselScalatestTester {
	object Tester {
		def apply[G <: Generator](gen: G, period: Int, resolution: Int)(block: (TableGen[G], Int => Unit) => Any) = {
			test(new TableGen(gen, period, resolution)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
				block(dut, value => {
					dut.io.out.expect(value.U)
					dut.clock.step()
				})
			}
		}
	}

	behavior of "TableGen"
	for (period <- 2 to 10 by 2) {
		for (resolution <- 2 to 10) {
			it should s"generate a square wave (p=$period, r=$resolution)" in {
				Tester(new SquareGenerator(), period, resolution) { (dut, expect) => 
					// dut.io.pause := false.B

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
