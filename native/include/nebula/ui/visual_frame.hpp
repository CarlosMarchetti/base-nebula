#pragma once
#include <array>
#include <chrono>
#include <memory>
#include <string>
#include <vector>
namespace nebula::gui {
inline constexpr size_t playerStride = 40, itemStride = 12;
struct VisualPlayer {
    std::array<std::string, 6> text;
    std::array<float, playerStride> data{};
};
struct VisualItem {
    std::string name;
    std::array<float, itemStride> data{};
};
struct VisualFrame {
    std::vector<VisualPlayer> players;
    std::vector<VisualItem> items;
    std::chrono::steady_clock::time_point received{}, projected{};
};
bool captureWorldMatrices(std::array<float,36>& values);
std::shared_ptr<const VisualFrame> sharedVisualFrame();
VisualFrame visualFrame();
void publishVisualFrame(VisualFrame frame, bool projected);
void renderVisuals();
bool hasVisualOverlays();
} 
