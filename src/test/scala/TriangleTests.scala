package wavegen

import chisel3._
import chisel3.experimental._
import chisel3.util._
import chiseltest._
import chisel3.experimental.BundleLiterals._
import org.scalatest.flatspec.AnyFlatSpec

class TriangleTests extends AnyFlatSpec with ChiselScalatestTester {
	implicit val clockFreq = 100_000_000

	behavior of "TriangleGen"
	it should "generate triangles" in {
		test(new TriangleGen(255)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
			dut.clock.setTimeout(0)
			dut.io.freq.poke(1_000_000)
			dut.io.pause.poke(false)
			for (i <- 0 until 10000) {
				dut.clock.step()
			}
		}
	}
}
