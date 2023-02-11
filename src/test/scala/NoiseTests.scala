package wavegen

import chisel3._
import chisel3.experimental._
import chisel3.util._
import chiseltest._
import chisel3.experimental.BundleLiterals._
import org.scalatest.flatspec.AnyFlatSpec

class NoiseTests extends AnyFlatSpec with ChiselScalatestTester {
	behavior of "NoiseGen"
	it should "generate noise" in {
		test(new NoiseGen(65536)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
			dut.clock.setTimeout(0)
			for (i <- 0 until 1000) {
				dut.clock.step()
			}
		}
	}
}
