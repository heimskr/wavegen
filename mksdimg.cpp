#include <fstream>
#include <iostream>
#include <sstream>
#include <string>
#include <vector>

int main(int argc, char **argv) {
	constexpr size_t TOC_MAX = 64;
	constexpr size_t TOC_ROW_LENGTH = 64;
	constexpr size_t NAME_LENGTH = 59;
	constexpr size_t ADDRESS_SIZE = 4;

	if (TOC_MAX + 1 < argc) {
		std::cerr << "Too many arguments\n";
		return 1;
	}

	std::stringstream out;
	std::vector<std::string> file_contents;

	size_t offset = TOC_MAX * TOC_ROW_LENGTH;
	size_t i = 1;

	for (; i < argc; ++i) {
		const std::string_view arg = argv[i];
		const size_t first_colon = arg.find_first_of(':');
		const size_t last_colon = arg.find_last_of(':');
		if (first_colon == std::string::npos) {
			std::cerr << "Colon not found in argument " << i << " (" << arg << ")\n";
			return 2;
		}

		if (first_colon == last_colon) {
			std::cerr << "Only one colon found in argument " << i << " (" << arg << ")\n";
			return 3;
		}

		if (last_colon != arg.size() - 2) {
			std::cerr << "Invalid final colon position in argument " << i << " (" << arg << ")\n";
			return 4;
		}

		const char back = arg.back();

		if (back != '1' && back != '2') {
			std::cerr << "Invalid APU type: '" << back << "'\n";
			return 5;
		}

		const auto name = arg.substr(first_colon + 1, last_colon - first_colon - 1);

		if (NAME_LENGTH < name.size()) {
			std::cerr << "Song name too long in argument " << i << " (" << arg << ")\n";
			return 6;
		}

		const auto filename = arg.substr(0, first_colon);
		std::ifstream ifstream {std::string(filename)}; // Most vexing parse moment

		if (!ifstream) {
			std::cerr << "Couldn't open \"" << filename << "\" for reading\n";
			return 7;
		}

		std::stringstream file_ss;
		file_ss << ifstream.rdbuf();

		out << static_cast<uint8_t>(back - '0') << name << std::string(NAME_LENGTH - name.size(), '\0');
		out << static_cast<uint8_t>(offset & 0xff);
		out << static_cast<uint8_t>((offset >> 8) & 0xff);
		out << static_cast<uint8_t>((offset >> 16) & 0xff);
		out << static_cast<uint8_t>((offset >> 24) & 0xff);
		file_contents.emplace_back(file_ss.str());
		offset += file_contents.back().size();
	}

	out << std::string(64 * (TOC_MAX + 1 - i), '\0');

	for (const auto &data: file_contents)
		out << data;

	std::cout << out.str();
}
