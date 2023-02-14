package wavegen

import chisel3._
import chisel3.experimental._
import chisel3.util._
import chiseltest._
import chisel3.experimental.BundleLiterals._
import org.scalatest.flatspec.AnyFlatSpec
import scala.util.control.Breaks._

class MixerTest1(period: Int = 40, resolution: Int = 1023) extends Module {
	val width = log2Ceil(resolution)

	val io = IO(new Bundle {
		val pause = Input(Bool())
		val c0v = Input(Bool())
		val c1v = Input(Bool())
		val c0r = Output(Bool())
		val c1r = Output(Bool())
		val out = Decoupled(FixedPoint(width.W, 0.BP))
		val debug = new MixerDebug(2, width)
	})

	val mixer = Module(new Mixer(2, width, resolution))
	val gen0 = Module(new TableGen(new SawtoothGenerator(true), period, resolution))
	val gen1 = Module(new TableGen(new TriangleGenerator(false), period * 2, resolution))

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
	io.debug <> mixer.io.debug
}

class MixerTest2(val channelCount: Int, val width: Int, val memorySize: Int) extends Module {
	val io = IO(new Bundle {
		val in = Flipped(Decoupled(Vec(channelCount, UInt(width.W))))
		val out = Decoupled(UInt(width.W))
		val debug = new MixerDebug(channelCount, width)
	})

	val mixer = Module(new Mixer(channelCount, width, memorySize))

	for (i <- 0 until channelCount) {
		mixer.io.in(i).valid := io.in.valid
		mixer.io.in(i).bits  := io.in.bits(i).asFixedPoint(0.BP)
	}

	io.in.ready := true.B
	io.out.valid := mixer.io.out.valid
	mixer.io.out.ready := io.out.ready
	io.out.bits  := mixer.io.out.bits.asUInt
	io.debug <> mixer.io.debug
}

class MixerTests extends AnyFlatSpec with ChiselScalatestTester {
	behavior of "Mixer"
	it should "mix" in {
		test(new MixerTest1).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
			dut.io.pause.poke(true.B)
			dut.io.c0v.poke(false.B)
			dut.io.c1v.poke(false.B)
			dut.io.out.valid.expect(false.B)
			dut.io.c0v.poke(true.B)
			dut.io.out.valid.expect(false.B)
			dut.io.c1v.poke(true.B)
			// dut.io.out.valid.expect(true.B)

			dut.io.pause.poke(false.B)

			dut.clock.setTimeout(0)
			breakable {
				for (i <- 0 until 10000) {
					dut.clock.step()
					if (dut.io.debug.state.peek() == 2)
						break()
				}
			}
		}
	}

	it should "mix random data properly" in {
		test(new MixerTest2(3, 8, 256)) { dut =>
			val chans = dut.channelCount
			val runs = dut.memorySize
			val randMax = 1 << dut.width
			val divisor = randMax - 1

			val data = Seq.tabulate(runs) { x =>
				Seq.tabulate(chans)(y => scala.util.Random.between(0, randMax))
			}

			val sums = data.map(_.sum)
			val max = sums.max
			val reduced = if (divisor < max) sums.map(_ * divisor / max) else sums

			dut.io.in.valid.poke(true)

			for (i <- 0 until runs) {
				for (c <- 0 until chans)
					dut.io.in.bits(c).poke(data(i)(c))
				dut.io.debug.state.expect(0)
				dut.clock.step()
			}

			dut.io.in.valid.poke(false)
			dut.io.debug.state.expect(1)
			dut.io.out.valid.expect(true)

			reduced.foreach { n =>
				dut.io.out.bits.expect(n)
				dut.clock.step()
			}
		}
	}
}
