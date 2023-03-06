package wavegen
package nes

import wavegen._
import chisel3._
import chisel3.experimental._
import chisel3.util._
import chiseltest._
import chisel3.experimental.BundleLiterals._
import org.scalatest.flatspec.AnyFlatSpec
import java.nio.file.{Files, Paths}
import scala.util.control.Breaks._

class NESTestModule(implicit clockFreq: Int) extends Module {
	implicit val inSimulator = true
	val addressWidth = 17
	val romWidth = 24

	val io = IO(new Bundle {
		val start   = Input(Bool())
		val rom     = Input(UInt(romWidth.W))
		val sw      = Input(UInt(8.W))
		val buttonU = Input(Bool())
		val buttonR = Input(Bool())
		val buttonD = Input(Bool())
		val buttonL = Input(Bool())
		val buttonC = Input(Bool())
		val addr    = Output(UInt(addressWidth.W))
		// val error   = Output(UInt(4.W))
		val leds    = Output(UInt(8.W))
		val audioL  = Output(UInt(7.W))
		val audioR  = Output(UInt(7.W))
	})

	// val rom = RegNext(VecInit(Files.readAllBytes(Paths.get("worldmap.vgm")).map(_.S(8.W).asUInt)))

	val nes = Module(new NES(addressWidth, romWidth))
	nes.io.tick   := DontCare
	nes.io.start  := io.start
	nes.io.rom    := io.rom
	nes.io.sw     := io.sw
	nes.io.pulseU := io.buttonU
	nes.io.pulseR := io.buttonR
	nes.io.pulseD := io.buttonD
	nes.io.pulseL := io.buttonL
	nes.io.pulseC := io.start
	io.addr       := nes.io.addr
	io.leds       := nes.io.leds
	io.audioL     := nes.io.outL
	io.audioR     := nes.io.outR
	// io.error      := nes.io.error
}

class NESTests extends AnyFlatSpec with ChiselScalatestTester {
	implicit val clockFreq   = 100_000_000

	behavior of "NES"
	it should "do something?" in {
		val bytes = Files.readAllBytes(Paths.get("mario.fpb"))
		val rom = Seq.tabulate(bytes.size / 3) { n =>
			((bytes(3 * n) & 0xff) << 16) | ((bytes(3 * n + 1) & 0xff) << 8) | (bytes(3 * n + 2) & 0xff)
		}

		test(new NESTestModule).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
			dut.io.sw.poke("b01111101".U)
			dut.io.buttonU.poke(false)
			dut.io.buttonR.poke(false)
			dut.io.buttonD.poke(false)
			dut.io.buttonL.poke(false)
			dut.io.buttonC.poke(false)
			dut.clock.step(10)
			dut.io.start.poke(true)
			dut.clock.setTimeout(0)
			dut.clock.step()
			dut.io.start.poke(false)
			var oldAddr: Int = -1

			def check(): Unit = {
				val addr = dut.io.addr.peek()
				val asInt = addr.litValue.toInt
				if (asInt != oldAddr) {
					dut.io.rom.poke(rom(asInt))
					// println(f"0x$oldAddr%x -> 0x$asInt%x : 0x${rom(asInt) & 0xff}%x")
					oldAddr = asInt
				}
			}

			val initialSteps = 500

			check()
			// dut.io.start.poke(true)

			for (i <- 0 until (initialSteps - 1)) {
				dut.clock.step()
				check()
			}

			// dut.io.start.poke(true)

			dut.clock.step()
			check()

			var x = true
			for (i <- initialSteps to 10_000_000) {
				dut.clock.step()
				check()
				if ((i % 250) == 0)
					println(i)
			}
		}
	}
}
