<div align="center">

  <img src="https://raw.githubusercontent.com/DrakesCraft-Labs/SensibleToolbox-drake/main/banner.svg" alt="SensibleToolbox-drake Banner" width="920" />

# 🧪 SensibleToolbox-Drake

**Modular automation, advanced item logistics, SCU energy systems, and industrial machinery for Slimefun4.**

<p>
  <a href="https://github.com/DrakesCraft-Labs/SensibleToolbox-drake"><img src="https://img.shields.io/badge/GitHub-SensibleToolbox--Drake-181717?style=for-the-badge&logo=github" alt="GitHub"/></a>
  <img src="https://img.shields.io/badge/Slimefun4-Drake_Edition-22C55E?style=for-the-badge&logo=curseforge&logoColor=white" alt="Slimefun4"/>
  <img src="https://img.shields.io/badge/Paper-1.21.11-38BDF8?style=for-the-badge&logo=minecraft&logoColor=white" alt="Paper 1.21.11"/>
  <img src="https://img.shields.io/badge/Java-21-F89820?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21"/>
</p>

[🇬🇧 **English**](README.md) · [🇪🇸 **Español**](README_ES.md)

</div>

> ### 🏰 Join the Official DrakesCraft Community!
> 
> * 🎮 **Server IP**: `mc.drakescraft.cl` *(Java 1.21.11 & Bedrock Port 25565 / 19132)*
> * 💬 **Official Discord**: [discord.gg/drakescraft](https://discord.gg/rv3vtXZTk7) — *Check out `#general-english`!*
> * 🌐 **Website & Guides**: [web.drakescraft.cl](https://web.drakescraft.cl) — 🛒 **Store**: [web.drakescraft.cl/store](https://web.drakescraft.cl/store.html)
> 
> *Play with this addon alongside 80+ optimized expansions live on our technical survival network!*

---

## 📖 What is SensibleToolbox-Drake?

**SensibleToolbox-Drake** is a comprehensive automation, logistics, and practical engineering expansion for **Slimefun4**. Designed to optimize complex factories, massive item storage arrays, directional pneumatic piping networks, and autonomous farms with zero TPS impact.

All items, machines, and tools are researched and crafted directly through the **Slimefun Guide (`/sf guide`)** under the *SensibleToolbox* category.

---

## ⚙️ Key Features & Machinery

### 🌾 1. Agricultural & Livestock Automation
* **AutoFarm & FastFarm**: Automated crop planting and harvesting with integrated `CombineHoe` durability support.
* **AutoForester**: Automatic tree chopping and sapling replanting for sustained lumber production.
* **AutoShearer**: Automated sheep shearing with direct extraction into internal machine storage.
* **Watering Can & Soil Saturation**: Technical watering cans that accelerate crops growth in surrounding farmland.

### 📦 2. Logistics, Filters & Item Routing
* **Item Router**: Intelligent multi-face distributor (North, South, East, West, Up, Down) with customizable input/output extraction rules.
* **Directional Hopper & Dense Pipe**: High-speed directional hoppers and dense piping networks for entity-free, lag-free bulk item routing.
* **Item Filter & Throttler**: Configurable whitelist/blacklist sorting modules and rate limiters to prevent buffer overflow.
* **BigStorageUnit & EnderStorageUnit (BSU / ESU)**: Massive single-item digital storage vaults holding hundreds of thousands of items with live digital indicators.
* **EnderPacker & EnderBox**: Quantum inventory packaging and teleportation linked via `EnderTuner`.

### ⚡ 3. SCU Power Grid (Sensible Charge Units)
* **SCU Power Grid & PowerBuffer**: Modular electrical networks with multi-conductor transfer cables and high-capacity battery banks.
* **BioEngine & FuelEngine**: Thermal generators fueled by biomass, biofuels, and combustible organic matter.
* **Solar Cell & Solar Panel Array**: Daylight solar collectors for passive recharge of portable capacitors and battery cells.

### 🔨 4. Automated Processing & Crafting
* **AutoSmelter & FastAutoSmelter**: High-throughput continuous smelting furnaces compatible with speed upgrade modules.
* **AutoAnvil & AutoDisenchanter**: Automatic tool repair and safe enchantment stripping directly into enchanted books.
* **Masher & Silicon Furnace**: Ore pulverization for ore-doubling yields and industrial silicon smelting for advanced electronics.
* **Thaumic Enchanter**: Arcane enchanting workstation applying advanced enchantments fueled by SCU energy.

### 🛠️ 5. Utility Tools & Construction
* **Multimeter & Tape Measure**: Real-time diagnostic meters measuring SCU energy throughput, ambient light levels, and exact 3D block distances.
* **MultiBuilder & Paint Brush / Roller**: Large-scale area construction tools and multi-surface block painting compatible with `PaintCan` pigments.
* **Sound Muffler**: Acoustic dampener block suppressing mechanical machine noise and mob farm sounds within a configurable radius.
* **Elevator & Ender Elevator**: Instantaneous vertical teleportation platforms between marked floor stages.

### ⚡ 6. Machine Upgrade Modules
* **Speed Upgrade**: Drastically accelerates machine tick execution in exchange for higher SCU energy consumption.
* **Regulator Upgrade**: Regulates and stabilizes extraction rates to prevent grid overdraw.
* **Ejector Upgrade**: Automatically pushes processed products into adjacent inventory containers.
* **Thoroughness Upgrade**: Maximizes yield efficiency per processing operation.

### 🤝 7. Social Trust Network
* **`/stb friend <player>`**: Grants trusted access to a registered player on your private STB networks and security vaults.
* **`/stb unfriend <player>`**: Revokes trust permissions.
* Fully validated with offline UUID resolution; invalid player names are rejected gracefully without causing server-side exceptions.

---

## 📋 Technical Compatibility

| Parameter | Requirement |
|---|---|
| **Server Software** | Paper / Purpur / Folia **1.21.11** |
| **Java Runtime** | **Java 21** LTS |
| **Required Core** | [Slimefun4-Drake](https://github.com/DrakesCraft-Labs/Slimefun4-Drake) |
| **Architecture** | 100% Server-Side (Vanilla Minecraft clients can join without installing client mods) |

---

## 📥 Installation

1. Download the latest release of `SensibleToolbox-drake.jar` from the [Versions](https://github.com/DrakesCraft-Labs/SensibleToolbox-drake/releases) page.
2. Place the `.jar` file into your server's `plugins/` directory alongside `Slimefun4-Drake.jar`.
3. Start or restart your server. Categories and recipes will automatically appear in `/sf guide`.

---

## 🛠️ Building from Source

```bash
git clone https://github.com/DrakesCraft-Labs/SensibleToolbox-drake.git
cd SensibleToolbox-drake
mvn clean package
```

The compiled artifact will be located under `target/SensibleToolbox-drake.jar`.

---

<div align="center">

**Developed and Maintained by [DrakesCraft Labs](https://github.com/DrakesCraft-Labs)**  
*Based on the original design by desht.*  
Licensed under **GPL-3.0-only**.

</div>

## ⚖️ Upstream Attribution & License

- **Original Project / Upstream**: Slimefun4 Community Addon (originally authored by desht).
- **Port & Maintenance**: DrakesCraft Labs team (Modernization and compatibility for Paper / Purpur 1.21.11 & Java 21).
- **License**: GNU General Public License v3.0 (GPL-3.0-only).
- **Source Code**: [GitHub Repository](https://github.com/DrakesCraft-Labs/SensibleToolbox-drake)
- **Support & Issues**: [GitHub Issues](https://github.com/DrakesCraft-Labs/SensibleToolbox-drake/issues) | [Discord](https://discord.gg/rv3vtXZTk7)

*This project is an open-source derivative work maintained by DrakesCraft Labs under the terms of its original license. All original assets, concepts, and trademarks belong to their respective creators.*
