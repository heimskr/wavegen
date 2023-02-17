package wavegen

import wavegen.gameboy._
import chisel3._
import chisel3.experimental._
import chisel3.util._
import chiseltest._
import chisel3.experimental.BundleLiterals._
import org.scalatest.flatspec.AnyFlatSpec
import java.nio.file.Files
import java.nio.file.Paths

class GameBoyTestModule(implicit clockFreq: Int) extends Module {
	val io = IO(new Bundle {
		val start = Input(Bool())
		val audio = Output(UInt(7.W))
	})
	
	val rom = RegNext(VecInit(Files.readAllBytes(Paths.get("worldmap.vgm")).map(_.S(8.W).asUInt)))

	val gameboy = Module(new GameBoy)
	gameboy.io.start := io.start
	gameboy.io.rom   := rom(gameboy.io.addr)
	io.audio := gameboy.io.out
}

class GameBoyTests extends AnyFlatSpec with ChiselScalatestTester {
	implicit val clockFreq = 100_000_000

	behavior of "GameBoy"
	it should "do something?" in {
		test(new GameBoyTestModule).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
			// dut.clock.step()
			// dut.io.trigger.poke(true)
			// println(f"Out[0]: ${dut.io.out.peek().litValue.toInt}")
			// dut.clock.step()
			// println(f"Out[1]: ${dut.io.out.peek().litValue.toInt}")
			// dut.clock.step()
			// println(f"Out[2]: ${dut.io.out.peek().litValue.toInt}")
			dut.clock.setTimeout(0)
			dut.io.start.poke(true)
			for (i <- 0 until 10000) {
				println(i)
				dut.clock.step()
			}
		}
	}
}
