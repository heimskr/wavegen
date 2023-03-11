package wavegen

import chisel3._
import chisel3.experimental._
import chisel3.util._
import chisel3.util.random._
import chiseltest._
import chisel3.experimental.BundleLiterals._
import org.scalatest.flatspec.AnyFlatSpec
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import wavegen.misc._

class OscilloscopeTestModule extends Module {
	val opts = OscilloscopeOpts(50, 4, 100, 32, 2, 0, 0, 8, 8)

	val io = IO(new Bundle {
		val out    = Valid(Bool())
		val sample = Valid(Bool())
		val x      = Output(UInt(8.W))
		val y      = Output(UInt(8.W))
		val debug  = Output(OscilloDebug(opts))
	})

	val scope = Module(new Oscilloscope(opts))

	val x = RegInit(0.U(8.W))
	val y = RegInit(0.U(8.W))

	when (x === 127.U) {
		x := 0.U
		when (y === 49.U) {
			y := 0.U
		} .otherwise {
			y := y + 1.U
		}
	} .otherwise {
		x := x + 1.U
	}

	scope.io.x := x
	scope.io.y := y
	io.x := x
	io.y := y

	scope.io.trigger := 8.U
	scope.io.slope   := true.B

	val lfsr = FibonacciLFSR(16, Set(1, 3, 7, 10))

	val (counter, wrap) = Counter(0 until 5)

	when (wrap) {
		scope.io.sampleIn.valid := true.B
		scope.io.sampleIn.bits  := lfsr(3, 0)
	} .otherwise {
		scope.io.sampleIn.valid := false.B
		scope.io.sampleIn.bits  := DontCare
	}

	io.out    := scope.io.out
	io.sample := scope.io.sampleIn
	io.debug  := scope.io.debug
}

class OscilloscopeTests extends AnyFlatSpec with ChiselScalatestTester {
	behavior of "Oscilloscope"
	it should "produce output" in {
		test(new OscilloscopeTestModule).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
			dut.clock.setTimeout(0)

			for (i <- 0 until 100000) {
				dut.clock.step()
				if (i % 250 == 0) {
					println(i)
				}
			}
		}
	}
}
