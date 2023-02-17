package wavegen.gameboy

import wavegen._
import chisel3._
import chisel3.experimental._
import chisel3.util._
import chiseltest._
import chisel3.experimental.BundleLiterals._
import org.scalatest.flatspec.AnyFlatSpec
import java.nio.file.{Files, Paths}
import scala.util.control.Breaks._

class GameBoyTestModule(implicit clockFreq: Int) extends Module {
	val io = IO(new Bundle {
		val start   = Input(Bool())
		val rom     = Input(UInt(8.W))
		val sw      = Input(UInt(8.W))
		val buttonU = Input(Bool())
		val buttonR = Input(Bool())
		val buttonD = Input(Bool())
		val buttonL = Input(Bool())
		val buttonC = Input(Bool())
		val addr    = Output(UInt(18.W))
		val error   = Output(UInt(4.W))
		val leds    = Output(UInt(8.W))
	})
	
	// val rom = RegNext(VecInit(Files.readAllBytes(Paths.get("worldmap.vgm")).map(_.S(8.W).asUInt)))

	val gameboy = Module(new GameBoy)
	gameboy.io.start   := io.start
	gameboy.io.rom     := io.rom
	gameboy.io.sw      := io.sw
	gameboy.io.buttonU := io.buttonU
	gameboy.io.buttonR := io.buttonR
	gameboy.io.buttonD := io.buttonD
	gameboy.io.buttonL := io.buttonL
	gameboy.io.buttonC := io.buttonC
	io.addr  := gameboy.io.addr
	io.error := gameboy.io.error
	io.leds  := gameboy.io.leds
}

class GameBoyTests extends AnyFlatSpec with ChiselScalatestTester {
	implicit val clockFreq = 100_000_000

	behavior of "GameBoy"
	it should "do something?" in {
		val rom = Files.readAllBytes(Paths.get("worldmap_dbg.vgm"))

		test(new GameBoyTestModule).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
			dut.io.sw.poke("b11111101".U)
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
					dut.io.rom.poke(rom(asInt) & 0xff)
					// println(f"0x$oldAddr%x -> 0x$asInt%x : 0x${rom(asInt) & 0xff}%x")
					oldAddr = asInt
				}
			}

			check()

			breakable {
				for (i <- 0 to 10000) {
					dut.clock.step()
					check()
					if ((i % 250) == 0)
						println(i)
					// if (dut.io.error.peek().litValue != 0)
					// 	break()
				}
			}
		}
	}
}
