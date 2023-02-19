package wavegen

import chisel3._
import chisel3.experimental._
import chisel3.util._
import chiseltest._
import chisel3.experimental.BundleLiterals._
import org.scalatest.flatspec.AnyFlatSpec


class ClockerTests extends AnyFlatSpec with ChiselScalatestTester {
	implicit val clockFreq = 100_000_000

	behavior of "Clocker"
	it should "produce a valid signal" in {
		test(new Clocker).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
			dut.io.enable.poke(true)
			dut.io.freq.valid.poke(true)
			dut.io.freq.bits.poke(10_000_000)

			dut.clock.setTimeout(0)

			for (i <- 0 to 500)
				dut.clock.step()

			dut.io.freq.valid.poke(false)

			for (i <- 0 to 500)
				dut.clock.step()
		}
	}
}
