package wavegen

import chisel3._
import chisel3.experimental._
import chisel3.util._
import chiseltest._
import chisel3.experimental.BundleLiterals._
import org.scalatest.flatspec.AnyFlatSpec

class Misc1 extends Module {
	val io = IO(new Bundle {
		val trigger = Input(Bool())
		val out = Output(UInt(8.W))
	})

	val reg = RegInit(0.U(8.W))

	io.out := 0.U

	when (io.trigger) {
		io.out := reg
		printf(p"[0] reg == $reg\n")
		reg := reg + 1.U
		printf(p"[1] reg == $reg\n")
	}
}

class MiscTests extends AnyFlatSpec with ChiselScalatestTester {
	implicit val clockFreq = 100_000_000

	behavior of "Misc"
	it should "misc" in {
		test(new Misc1) { dut =>
			dut.clock.step()
			dut.io.trigger.poke(true)
			println(f"Out[0]: ${dut.io.out.peek().litValue.toInt}")
			dut.clock.step()
			println(f"Out[1]: ${dut.io.out.peek().litValue.toInt}")
			dut.clock.step()
			println(f"Out[2]: ${dut.io.out.peek().litValue.toInt}")
		}
	}
}
