package wavegen

import chisel3._
import chisel3.experimental._
import chisel3.util._
import chiseltest._
import chisel3.experimental.BundleLiterals._
import org.scalatest.flatspec.AnyFlatSpec

class MixerTest1(width: Int = 8) extends Module {
	val io = IO(new Bundle {
		val pause = Input(Bool())
		val c0v = Input(Bool())
		val c1v = Input(Bool())
		val c0r = Output(Bool())
		val c1r = Output(Bool())
		val out = Decoupled(FixedPoint(width.W, 0.BP))
	})

	val mixer = Module(new Mixer(2, width, 255))
	val gen0 = Module(new TableGen(new SawtoothGenerator(true), 10, 255))
	val gen1 = Module(new TableGen(new TriangleGenerator(true), 10, 255))

	gen0.io.pause := io.pause
	gen1.io.pause := io.pause

	val c0 = mixer.io.in(0)
	val c1 = mixer.io.in(1)

	c0.bits := gen0.io.out.asFixedPoint(0.BP)
	c1.bits := gen1.io.out.asFixedPoint(0.BP)

	c0.valid := io.c0v
	c1.valid := io.c1v

	io.c0r := c0.ready
	io.c1r := c1.ready

	io.out <> mixer.io.out
}

class MixerTests extends AnyFlatSpec with ChiselScalatestTester {
	behavior of "Mixer"
	it should "mix properly" in {
		test(new MixerTest1).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
			dut.io.pause.poke(true.B)
			dut.io.c0v.poke(false.B)
			dut.io.c1v.poke(false.B)
			dut.io.out.valid.expect(false.B)
			dut.io.c0v.poke(true.B)
			dut.io.out.valid.expect(false.B)
			dut.io.c1v.poke(true.B)
			// dut.io.out.valid.expect(true.B)
		}
	}
}
