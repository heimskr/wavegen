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

class Misc2 extends Module {
	val io = IO(new Bundle {
		val in  = Input(UInt(8.W))
		val out = Output(UInt(8.W))
	})

	// io.out := PriorityEncoder(io.in)

	def leadingZeros(x: UInt): UInt = {
		val out = Wire(chiselTypeOf(x))
		val priority = PriorityEncoder(Reverse(x))

		when (x === 0.U) {
			out := x.getWidth.U
		} .otherwise {
			out := priority
		}

		out
	}

	io.out := leadingZeros(io.in)
}

class MiscTests extends AnyFlatSpec with ChiselScalatestTester {
	implicit val clockFreq = 100_000_000

	behavior of "Misc"
	it should "misc1" in {
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

	it should "misc2" in {
		test(new Misc2) { dut =>
			def toBinary(i: Int, digits: Int = 8) = String.format("%" + digits + "s", i.toBinaryString).replace(' ', '0')
			for (i <- 0 to 255) {
				dut.io.in.poke(i)
				dut.clock.step()
				println(f"${toBinary(i)} -> ${dut.io.out.peek().litValue.toInt}")
			}
		}
	}
}
