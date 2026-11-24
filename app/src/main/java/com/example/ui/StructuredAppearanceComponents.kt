package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val PtBrLocale = Locale.forLanguageTag("pt-BR")

/**
 * Abre um endereço ou link compartilhado do Google Maps (ex.: maps.app.goo.gl / google.com/maps)
 * diretamente no aplicativo Google Maps ou navegador.
 */
fun openLocationInGoogleMaps(context: Context, rawLocation: String) {
    val clean = rawLocation.trim()
    if (clean.isEmpty()) return

    val isWebUrl = clean.startsWith("http://", ignoreCase = true) ||
        clean.startsWith("https://", ignoreCase = true) ||
        clean.contains("maps.app.goo.gl", ignoreCase = true) ||
        clean.contains("google.com/maps", ignoreCase = true) ||
        clean.contains("goo.gl/maps", ignoreCase = true)

    if (isWebUrl) {
        val normalizedUrl = if (clean.startsWith("http://", ignoreCase = true) || clean.startsWith("https://", ignoreCase = true)) {
            clean
        } else {
            "https://$clean"
        }
        val openedUrl = runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalizedUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
        if (openedUrl) return
    }

    val geoUri = Uri.parse("geo:0,0?q=${Uri.encode(clean)}")
    val openedGeo = runCatching {
        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(mapIntent)
        true
    }.getOrDefault(false)

    if (!openedGeo) {
        val fallbackGeo = runCatching {
            val genericGeoIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(genericGeoIntent)
            true
        }.getOrDefault(false)

        if (!fallbackGeo) {
            runCatching {
                val webMapsUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(clean)}")
                val webIntent = Intent(Intent.ACTION_VIEW, webMapsUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            }
        }
    }
}

fun formatTaskDurationHumanPt(durationMinutes: Int): String {
    val safe = durationMinutes.coerceAtLeast(5)
    val h = safe / 60
    val m = safe % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m} min"
    }
}

/**
 * Reusable Expandable Section Card to keep screens clean and organized.
 */
@Composable
fun AuraExpandableSectionCard(
    title: String,
    subtitle: String = "",
    icon: ImageVector,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    badgeText: String? = null,
    initiallyExpanded: Boolean = false,
    testTag: String = "expandable_section",
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val organicStyle = com.example.ui.theme.LocalOrganicVisualStyle.current
    val cardRadius = com.example.ui.theme.LocalOrganicCardRadius.current
    val organicShape = RoundedCornerShape(cardRadius)
    val borderStroke = com.example.ui.theme.omniOrganicCardBorder(accentColor = accentColor, style = organicStyle)
    val cardElevation = com.example.ui.theme.omniOrganicCardElevationDp(defaultDp = if (expanded) 4.dp else 1.dp, style = organicStyle)
    val containerSurfaceColor = when (organicStyle) {
        OrganicVisualStyle.TRANSLUCIDO_GLASS -> MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = borderStroke.width,
                brush = borderStroke.brush,
                shape = organicShape
            )
            .testTag(testTag),
        shape = organicShape,
        colors = CardDefaults.cardColors(
            containerColor = containerSurfaceColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (organicStyle == OrganicVisualStyle.MINIMALISTA_ACOLHEDOR) {
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(containerSurfaceColor, containerSurfaceColor)
                        )
                    } else {
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = if (expanded) 0.12f else 0.06f),
                                containerSurfaceColor,
                                containerSurfaceColor
                            )
                        )
                    }
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 18.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                androidx.compose.ui.graphics.Brush.radialGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = 0.28f),
                                        accentColor.copy(alpha = 0.12f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (subtitle.isNotBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!badgeText.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = accentColor.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.10f),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (expanded) "Recolher $title" else "Expandir $title",
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = accentColor.copy(alpha = 0.16f))
                    content()
                }
            }
        }
    }
}

data class StructuredTaskIconEntry(
    val emoji: String,
    val label: String,
    val category: String,
    val vector: ImageVector,
    val keywords: String = ""
)

data class StructuredTaskIconCategory(
    val id: String,
    val title: String,
    val badgeEmoji: String
)

object StructuredTaskIconCatalog {
    val categories: List<StructuredTaskIconCategory> = listOf(
        StructuredTaskIconCategory("ALL", "Todos", "✨"),
        StructuredTaskIconCategory("Casa", "Casa", "🏠"),
        StructuredTaskIconCategory("Trabalho", "Trabalho", "💼"),
        StructuredTaskIconCategory("Saúde", "Saúde", "🩺"),
        StructuredTaskIconCategory("Finanças", "Finanças", "💳"),
        StructuredTaskIconCategory("Família", "Família", "👨‍👩‍👧‍👦"),
        StructuredTaskIconCategory("Compras", "Compras", "🛒"),
        StructuredTaskIconCategory("Estudos", "Estudos", "📚"),
        StructuredTaskIconCategory("Lazer", "Lazer", "🎮"),
        StructuredTaskIconCategory("Transporte", "Transporte", "🚗"),
        StructuredTaskIconCategory("Alimentação", "Alimentação", "🍽️"),
        StructuredTaskIconCategory("Pets", "Pets", "🐾"),
        StructuredTaskIconCategory("Rotina", "Rotina", "⏰")
    )

    val allIcons: List<StructuredTaskIconEntry> = listOf(
        // Rotina
        StructuredTaskIconEntry("⏰", "Acordar / Alarme", "Rotina", Icons.Default.AccessAlarm, "despertador manhã levantar horario"),
        StructuredTaskIconEntry("☀️", "Rotina da Manhã", "Rotina", Icons.Default.WbSunny, "sol dia acordar"),
        StructuredTaskIconEntry("🌙", "Dormir / Sono", "Rotina", Icons.Default.Bedtime, "noite descanso cama"),
        StructuredTaskIconEntry("📋", "Lista / Checklist", "Rotina", Icons.Default.Checklist, "tarefa rotina afazeres"),
        StructuredTaskIconEntry("🚿", "Banho / Higiene", "Rotina", Icons.Default.Shower, "autocuidado banho"),
        StructuredTaskIconEntry("🪥", "Escovar Dentes", "Rotina", Icons.Default.CleanHands, "dentes higiene"),
        StructuredTaskIconEntry("🧘", "Meditação / Pausa", "Rotina", Icons.Default.SelfImprovement, "respirar yoga relaxar calma"),
        StructuredTaskIconEntry("📔", "Diário / Reflexão", "Rotina", Icons.Default.MenuBook, "escrever notas gratidão"),
        StructuredTaskIconEntry("💧", "Beber Água", "Rotina", Icons.Default.WaterDrop, "hidratação copo"),
        StructuredTaskIconEntry("⚡", "Foco / Energia", "Rotina", Icons.Default.Bolt, "produtividade"),
        StructuredTaskIconEntry("⭐", "Inegociável / Meta", "Rotina", Icons.Default.Star, "essencial importante"),
        StructuredTaskIconEntry("🔔", "Lembrete Rápido", "Rotina", Icons.Default.NotificationsActive, "aviso alerta"),

        // Casa
        StructuredTaskIconEntry("🏠", "Casa / Lar", "Casa", Icons.Default.Home, "moradia apartamento"),
        StructuredTaskIconEntry("🏡", "Jardim / Quintal", "Casa", Icons.Default.Cottage, "plantas área externa"),
        StructuredTaskIconEntry("🧹", "Limpar a Casa", "Casa", Icons.Default.CleaningServices, "faxina varrer arrumar"),
        StructuredTaskIconEntry("🧺", "Lavar Roupa", "Casa", Icons.Default.LocalLaundryService, "lavanderia secar passar"),
        StructuredTaskIconEntry("🛏️", "Arrumar Quarto", "Casa", Icons.Default.Bed, "cama lençol"),
        StructuredTaskIconEntry("🛋️", "Sala / Organização", "Casa", Icons.Default.Weekend, "sofá sala"),
        StructuredTaskIconEntry("🔧", "Conserto / Reparo", "Casa", Icons.Default.Build, "manutenção ferramenta obra"),
        StructuredTaskIconEntry("🪴", "Regar Plantas", "Casa", Icons.Default.Spa, "flores horta"),
        StructuredTaskIconEntry("🗑️", "Tirar o Lixo", "Casa", Icons.Default.Delete, "reciclagem limpeza"),
        StructuredTaskIconEntry("💡", "Energia / Luz", "Casa", Icons.Default.Lightbulb, "lâmpada iluminação"),
        StructuredTaskIconEntry("🔑", "Chaves / Portaria", "Casa", Icons.Default.Key, "condomínio segurança"),
        StructuredTaskIconEntry("📦", "Encomenda / Entrega", "Casa", Icons.Default.Inventory2, "correios pacote"),

        // Trabalho
        StructuredTaskIconEntry("💼", "Trabalho / Escritório", "Trabalho", Icons.Default.Work, "emprego empresa"),
        StructuredTaskIconEntry("💻", "Computador / Dev", "Trabalho", Icons.Default.LaptopMac, "notebook código home office"),
        StructuredTaskIconEntry("📊", "Relatório / Planilha", "Trabalho", Icons.Default.BarChart, "dados apresentação"),
        StructuredTaskIconEntry("📞", "Ligação / Chamada", "Trabalho", Icons.Default.Call, "telefone cliente"),
        StructuredTaskIconEntry("📹", "Reunião Online", "Trabalho", Icons.Default.Videocam, "meet zoom call video"),
        StructuredTaskIconEntry("📧", "Responder E-mails", "Trabalho", Icons.Default.Email, "mensagem inbox"),
        StructuredTaskIconEntry("📅", "Agenda / Planejamento", "Trabalho", Icons.Default.CalendarMonth, "cronograma sprint"),
        StructuredTaskIconEntry("📝", "Contrato / Documento", "Trabalho", Icons.Default.Description, "assinar proposta"),
        StructuredTaskIconEntry("🎯", "Meta / Projeto", "Trabalho", Icons.Default.Flag, "entrega prazo"),
        StructuredTaskIconEntry("🤝", "Cliente / Parceria", "Trabalho", Icons.Default.Groups, "negócio reunião presencial"),
        StructuredTaskIconEntry("🖨️", "Imprimir / Arquivos", "Trabalho", Icons.Default.Print, "papel xerox"),
        StructuredTaskIconEntry("🚀", "Lançamento / Entrega", "Trabalho", Icons.Default.RocketLaunch, "deploy meta"),

        // Saúde
        StructuredTaskIconEntry("🩺", "Consulta Médica", "Saúde", Icons.Default.MedicalServices, "médico doutor hospital clínica"),
        StructuredTaskIconEntry("💊", "Remédio / Vitamina", "Saúde", Icons.Default.Medication, "comprimido medicação dose"),
        StructuredTaskIconEntry("🩸", "Medir Glicemia", "Saúde", Icons.Default.Bloodtype, "diabetes açúcar sangue insulina"),
        StructuredTaskIconEntry("❤️", "Pressão Arterial", "Saúde", Icons.Default.MonitorHeart, "cardio coração batimentos"),
        StructuredTaskIconEntry("🏋️", "Academia / Musculação", "Saúde", Icons.Default.FitnessCenter, "treino peso exercício"),
        StructuredTaskIconEntry("🏃", "Corrida / Caminhada", "Saúde", Icons.Default.DirectionsRun, "cardio esporte passo"),
        StructuredTaskIconEntry("🚴", "Pedalar / Bike", "Saúde", Icons.Default.DirectionsBike, "bicicleta ciclismo"),
        StructuredTaskIconEntry("🦷", "Dentista", "Saúde", Icons.Default.HealthAndSafety, "odonto dente"),
        StructuredTaskIconEntry("🧠", "Terapia / Psicólogo", "Saúde", Icons.Default.Psychology, "saúde mental sessão"),
        StructuredTaskIconEntry("🤰", "Pré-natal / Gestação", "Saúde", Icons.Default.PregnantWoman, "obstetra ultrassom bebê"),
        StructuredTaskIconEntry("🩹", "Exame / Laboratório", "Saúde", Icons.Default.Healing, "coleta sangue checkup"),
        StructuredTaskIconEntry("🧘‍♀️", "Alongamento / Pilates", "Saúde", Icons.Default.AccessibilityNew, "fisioterapia postura"),

        // Finanças
        StructuredTaskIconEntry("💳", "Pagar Fatura / Cartão", "Finanças", Icons.Default.CreditCard, "crédito banco nubank"),
        StructuredTaskIconEntry("💰", "Receber / Salário", "Finanças", Icons.Default.Paid, "renda dinheiro entrada"),
        StructuredTaskIconEntry("🏦", "Banco / Transferência", "Finanças", Icons.Default.AccountBalance, "pix ted conta"),
        StructuredTaskIconEntry("🧾", "Boleto / Conta Fixa", "Finanças", Icons.Default.ReceiptLong, "água luz internet aluguel"),
        StructuredTaskIconEntry("📈", "Investimento / Reserva", "Finanças", Icons.Default.TrendingUp, "poupança ações caixinha"),
        StructuredTaskIconEntry("🪙", "Economia / Cofre", "Finanças", Icons.Default.Savings, "guardar meta financeira"),
        StructuredTaskIconEntry("💸", "Despesa / Pagamento", "Finanças", Icons.Default.AccountBalanceWallet, "gasto saída"),
        StructuredTaskIconEntry("🧮", "Revisão de Orçamento", "Finanças", Icons.Default.Calculate, "fechamento mês 50/30/20"),
        StructuredTaskIconEntry("🏷️", "Imposto / Taxa", "Finanças", Icons.Default.LocalOffer, "ipva iptu irpf"),
        StructuredTaskIconEntry("🔐", "Seguro / Proteção", "Finanças", Icons.Default.Security, "vida carro residencial"),
        StructuredTaskIconEntry("🤝", "Reembolso / Dividir", "Finanças", Icons.Default.SwapHoriz, "acertar contas"),
        StructuredTaskIconEntry("💎", "Meta Conquistada", "Finanças", Icons.Default.WorkspacePremium, "objetivo patrimônio"),

        // Família
        StructuredTaskIconEntry("👨‍👩‍👧‍👦", "Momento em Família", "Família", Icons.Default.FamilyRestroom, "casa juntos"),
        StructuredTaskIconEntry("👶", "Cuidar do Bebê", "Família", Icons.Default.ChildCare, "filho fralda mamadeira"),
        StructuredTaskIconEntry("🧒", "Levar/Buscar na Escola", "Família", Icons.Default.ChildFriendly, "criança colégio"),
        StructuredTaskIconEntry("💑", "Encontro / Casal", "Família", Icons.Default.Favorite, "namoro jantar aniversário"),
        StructuredTaskIconEntry("🎂", "Aniversário / Festa", "Família", Icons.Default.Cake, "comemoração parabéns"),
        StructuredTaskIconEntry("👵", "Visitar Pais / Avós", "Família", Icons.Default.Elderly, "parentes família"),
        StructuredTaskIconEntry("🎁", "Comprar Presente", "Família", Icons.Default.CardGiftcard, "lembrança surpresa"),
        StructuredTaskIconEntry("📸", "Fotos / Memórias", "Família", Icons.Default.PhotoCamera, "álbum passeio"),
        StructuredTaskIconEntry("🧸", "Brincar com Crianças", "Família", Icons.Default.Toys, "jogos parque"),
        StructuredTaskIconEntry("🍼", "Pediatra / Vacina", "Família", Icons.Default.Vaccines, "saúde infantil"),
        StructuredTaskIconEntry("🏡", "Reunião Familiar", "Família", Icons.Default.Cottage, "almoço domingo"),
        StructuredTaskIconEntry("💬", "Conversar / Alinhar", "Família", Icons.Default.Forum, "combinado rotina"),

        // Compras
        StructuredTaskIconEntry("🛒", "Supermercado", "Compras", Icons.Default.ShoppingCart, "mercado feira compras mês"),
        StructuredTaskIconEntry("🛍️", "Shopping / Roupas", "Compras", Icons.Default.ShoppingBag, "loja vestuário"),
        StructuredTaskIconEntry("🥖", "Padaria", "Compras", Icons.Default.BakeryDining, "pão café leite"),
        StructuredTaskIconEntry("🥬", "Hortifruti / Feira", "Compras", Icons.Default.Eco, "frutas verduras legumes"),
        StructuredTaskIconEntry("🥩", "Açougue", "Compras", Icons.Default.Storefront, "carne churrasco"),
        StructuredTaskIconEntry("💊", "Farmácia", "Compras", Icons.Default.LocalPharmacy, "drogaria remédio higiene"),
        StructuredTaskIconEntry("💻", "Compra Online", "Compras", Icons.Default.LocalMall, "site app mercado livre"),
        StructuredTaskIconEntry("🎁", "Presentes", "Compras", Icons.Default.CardGiftcard, "aniversário natal"),
        StructuredTaskIconEntry("🧴", "Produtos de Limpeza", "Compras", Icons.Default.CleanHands, "sabão detergente"),
        StructuredTaskIconEntry("👟", "Calçados / Esporte", "Compras", Icons.Default.Checkroom, "tênis roupa"),
        StructuredTaskIconEntry("🛋️", "Móveis / Decoração", "Compras", Icons.Default.Chair, "casa utensílios"),
        StructuredTaskIconEntry("📱", "Eletrônicos", "Compras", Icons.Default.Smartphone, "celular acessório"),

        // Estudos
        StructuredTaskIconEntry("📚", "Estudar / Leitura", "Estudos", Icons.Default.MenuBook, "livro capítulo apostila"),
        StructuredTaskIconEntry("🎓", "Aula / Faculdade", "Estudos", Icons.Default.School, "curso pós universidade"),
        StructuredTaskIconEntry("✍️", "Exercícios / Redação", "Estudos", Icons.Default.EditNote, "resumo prova simulado"),
        StructuredTaskIconEntry("🗣️", "Idiomas / Inglês", "Estudos", Icons.Default.Translate, "língua conversação"),
        StructuredTaskIconEntry("🎧", "Videoaula / Podcast", "Estudos", Icons.Default.Headphones, "curso online ouvir"),
        StructuredTaskIconEntry("🧪", "Pesquisa / Laboratório", "Estudos", Icons.Default.Science, "ciência artigo"),
        StructuredTaskIconEntry("📝", "Prova / Exame", "Estudos", Icons.Default.Quiz, "avaliação concurso"),
        StructuredTaskIconEntry("💡", "Revisão / Flashcards", "Estudos", Icons.Default.Lightbulb, "memorizar estudo"),
        StructuredTaskIconEntry("🏛️", "Biblioteca", "Estudos", Icons.Default.AccountBalance, "leitura silêncio"),
        StructuredTaskIconEntry("💻", "Curso de Programação", "Estudos", Icons.Default.Code, "dev tecnologia"),
        StructuredTaskIconEntry("📐", "Matemática / Exatas", "Estudos", Icons.Default.Calculate, "cálculo lógica"),
        StructuredTaskIconEntry("🏆", "Certificado / TCC", "Estudos", Icons.Default.EmojiEvents, "diploma entrega"),

        // Lazer
        StructuredTaskIconEntry("🎮", "Videogame / Jogos", "Lazer", Icons.Default.SportsEsports, "jogar console pc"),
        StructuredTaskIconEntry("🎬", "Filme / Série / Cinema", "Lazer", Icons.Default.Movie, "streaming tv pipoca"),
        StructuredTaskIconEntry("🎵", "Música / Violão", "Lazer", Icons.Default.MusicNote, "instrumento show playlist"),
        StructuredTaskIconEntry("🏖️", "Praia / Piscina", "Lazer", Icons.Default.BeachAccess, "sol férias descanso"),
        StructuredTaskIconEntry("🌳", "Parque / Ar Livre", "Lazer", Icons.Default.Park, "natureza caminhada piquenique"),
        StructuredTaskIconEntry("⚽", "Futebol / Esporte", "Lazer", Icons.Default.SportsSoccer, "jogo quadra amigos"),
        StructuredTaskIconEntry("🎨", "Arte / Desenho / Hobby", "Lazer", Icons.Default.Palette, "pintura artesanato"),
        StructuredTaskIconEntry("📷", "Fotografia", "Lazer", Icons.Default.PhotoCamera, "passeio câmera"),
        StructuredTaskIconEntry("🎉", "Sair com Amigos", "Lazer", Icons.Default.Celebration, "bar festa encontro"),
        StructuredTaskIconEntry("🏕️", "Viagem / Passeio", "Lazer", Icons.Default.Terrain, "trilha fim de semana"),
        StructuredTaskIconEntry("📖", "Leitura por Lazer", "Lazer", Icons.Default.AutoStories, "romance ficção"),
        StructuredTaskIconEntry("🧖", "Spa / Autocuidado", "Lazer", Icons.Default.Spa, "massagem skincare"),

        // Transporte
        StructuredTaskIconEntry("🚗", "Carro / Dirigir", "Transporte", Icons.Default.DirectionsCar, "trânsito deslocamento"),
        StructuredTaskIconEntry("⛽", "Abastecer Combustível", "Transporte", Icons.Default.LocalGasStation, "posto gasolina etanol"),
        StructuredTaskIconEntry("🚌", "Ônibus / Transporte", "Transporte", Icons.Default.DirectionsBus, "público linha"),
        StructuredTaskIconEntry("🚇", "Metrô / Trem", "Transporte", Icons.Default.Subway, "estação trilho"),
        StructuredTaskIconEntry("🚕", "Uber / Táxi / App", "Transporte", Icons.Default.LocalTaxi, "corrida carona"),
        StructuredTaskIconEntry("✈️", "Voo / Aeroporto", "Transporte", Icons.Default.Flight, "viagem avião check-in"),
        StructuredTaskIconEntry("🏍️", "Moto", "Transporte", Icons.Default.TwoWheeler, "capacete entrega"),
        StructuredTaskIconEntry("🔧", "Oficina / Revisão", "Transporte", Icons.Default.CarRepair, "mecânico óleo pneu"),
        StructuredTaskIconEntry("🧼", "Lavar o Carro", "Transporte", Icons.Default.LocalCarWash, "lava-rápido limpeza"),
        StructuredTaskIconEntry("🅿️", "Estacionamento", "Transporte", Icons.Default.LocalParking, "vaga zona azul"),
        StructuredTaskIconEntry("🗺️", "Rota / Endereço", "Transporte", Icons.Default.Map, "gps localização"),
        StructuredTaskIconEntry("🚲", "Bicicleta / Patinete", "Transporte", Icons.Default.PedalBike, "ciclovia"),

        // Alimentação
        StructuredTaskIconEntry("🍽️", "Almoço / Jantar", "Alimentação", Icons.Default.Restaurant, "refeição prato comida"),
        StructuredTaskIconEntry("☕", "Café da Manhã / Lanche", "Alimentação", Icons.Default.Coffee, "chá expresso tarde"),
        StructuredTaskIconEntry("🍳", "Cozinhar / Marmita", "Alimentação", Icons.Default.SoupKitchen, "preparar receita fogão"),
        StructuredTaskIconEntry("🥗", "Salada / Dieta Saudável", "Alimentação", Icons.Default.RestaurantMenu, "nutrição leve"),
        StructuredTaskIconEntry("🍎", "Fruta / Lanche Natural", "Alimentação", Icons.Default.LocalDining, "fibra vitamina"),
        StructuredTaskIconEntry("🍕", "Delivery / Pizza", "Alimentação", Icons.Default.LocalPizza, "ifood lanche noite"),
        StructuredTaskIconEntry("🍔", "Hambúrguer / Fast Food", "Alimentação", Icons.Default.Fastfood, "sanduíche"),
        StructuredTaskIconEntry("🍰", "Sobremesa / Doce", "Alimentação", Icons.Default.Icecream, "bolo confeitaria"),
        StructuredTaskIconEntry("🍷", "Jantar Especial", "Alimentação", Icons.Default.WineBar, "restaurante reserva"),
        StructuredTaskIconEntry("🥤", "Suco / Shake / Whey", "Alimentação", Icons.Default.LocalCafe, "suplemento bebida"),
        StructuredTaskIconEntry("🍲", "Congelar Refeições", "Alimentação", Icons.Default.Kitchen, "geladeira semana"),
        StructuredTaskIconEntry("🥐", "Café na Padaria", "Alimentação", Icons.Default.BakeryDining, "pão de queijo"),

        // Pets
        StructuredTaskIconEntry("🐾", "Passear com o Pet", "Pets", Icons.Default.Pets, "cachorro guia rua"),
        StructuredTaskIconEntry("🐶", "Alimentar Cachorro", "Pets", Icons.Default.Pets, "ração água cão"),
        StructuredTaskIconEntry("🐱", "Cuidar do Gato", "Pets", Icons.Default.Pets, "caixa de areia sachê"),
        StructuredTaskIconEntry("🛁", "Banho & Tosa (Petshop)", "Pets", Icons.Default.Shower, "higiene pet"),
        StructuredTaskIconEntry("💉", "Veterinário / Vacina Pet", "Pets", Icons.Default.Vaccines, "consulta vermífugo antipulgas"),
        StructuredTaskIconEntry("🦴", "Comprar Ração / Petiscos", "Pets", Icons.Default.ShoppingBag, "petshop brinquedo"),
        StructuredTaskIconEntry("🎾", "Brincar com o Pet", "Pets", Icons.Default.SportsBaseball, "parque bolinha"),
        StructuredTaskIconEntry("💊", "Remédio do Pet", "Pets", Icons.Default.Medication, "tratamento"),
        StructuredTaskIconEntry("✂️", "Cortar Unhas / Escovar", "Pets", Icons.Default.ContentCut, "pelos cuidado"),
        StructuredTaskIconEntry("🏠", "Limpar Espaço do Pet", "Pets", Icons.Default.CleaningServices, "casinha tapete higiênico")
    )
}

/**
 * Resolve Task Icon ImageVector from task iconEmoji or category/title.
 */
fun resolveStructuredTaskIcon(task: TaskItem): ImageVector {
    val emoji = task.iconEmoji.trim()
    val catalogMatch = StructuredTaskIconCatalog.allIcons.firstOrNull { it.emoji == emoji }
    if (catalogMatch != null) return catalogMatch.vector

    val titleLower = task.title.lowercase()
    return when {
        emoji == "⏰" || titleLower.contains("acordar") || titleLower.contains("levantar") -> Icons.Default.AccessAlarm
        emoji == "🍽️" || emoji == "☕" || titleLower.contains("almoço") || titleLower.contains("jantar") || titleLower.contains("café") || titleLower.contains("refeição") -> Icons.Default.Restaurant
        emoji == "💻" || task.category.equals("Trabalho", ignoreCase = true) || titleLower.contains("trabalhar") || titleLower.contains("reunião") -> Icons.Default.LaptopMac
        emoji == "💊" || task.category.equals("Saúde", ignoreCase = true) || titleLower.contains("remédio") || titleLower.contains("vitamina") || titleLower.contains("glicemia") -> Icons.Default.Medication
        emoji == "🏠" || task.category.equals("Casa", ignoreCase = true) || titleLower.contains("casa") || titleLower.contains("limpar") -> Icons.Default.Home
        emoji == "👨‍👩‍👧‍👦" || task.category.equals("Família", ignoreCase = true) -> Icons.Default.FamilyRestroom
        emoji == "💳" || task.category.equals("Finanças", ignoreCase = true) || task.estimatedCostBrl > 0.0 -> Icons.Default.AccountBalanceWallet
        emoji == "🛒" || task.category.equals("Compras", ignoreCase = true) || titleLower.contains("mercado") -> Icons.Default.ShoppingCart
        emoji == "📚" || task.category.equals("Estudos", ignoreCase = true) || titleLower.contains("estudar") -> Icons.Default.MenuBook
        emoji == "🎮" || task.category.equals("Lazer", ignoreCase = true) -> Icons.Default.SportsEsports
        emoji == "🚗" || task.category.equals("Transporte", ignoreCase = true) -> Icons.Default.DirectionsCar
        emoji == "🐾" || task.category.equals("Pets", ignoreCase = true) -> Icons.Default.Pets
        emoji == "🌙" || titleLower.contains("dormir") || titleLower.contains("sono") -> Icons.Default.Bedtime
        emoji == "📞" || titleLower.contains("ligar") -> Icons.Default.Call
        else -> Icons.Default.CheckCircleOutline
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StructuredCategorizedIconPicker(
    selectedEmoji: String,
    accentColor: Color,
    onSelectIcon: (StructuredTaskIconEntry) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("ALL") }

    val filteredIcons = remember(selectedCategory) {
        StructuredTaskIconCatalog.allIcons.filter { entry ->
            selectedCategory == "ALL" || entry.category.equals(selectedCategory, ignoreCase = true)
        }
    }

    val currentSelectedEntry = remember(selectedEmoji) {
        StructuredTaskIconCatalog.allIcons.firstOrNull { it.emoji == selectedEmoji }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("structured_categorized_icon_picker")
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ícone por Categoria",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentSelectedEntry?.let { "${it.emoji} ${it.label} (${it.category})" }
                            ?: "Ícone selecionado: $selectedEmoji",
                        style = MaterialTheme.typography.bodySmall,
                        color = accentColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = currentSelectedEntry?.vector ?: Icons.Default.CheckCircleOutline,
                        contentDescription = null,
                        tint = Color(0xFF1A1B1E),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Category Filter Chips (compact single horizontal row)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StructuredTaskIconCatalog.categories.forEach { cat ->
                    val isSel = selectedCategory == cat.id
                    FilterChip(
                        selected = isSel,
                        onClick = { selectedCategory = cat.id },
                        label = { Text("${cat.badgeEmoji} ${cat.title}", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("icon_cat_tab_${cat.id}")
                    )
                }
            }

            // Compact Grid of icons
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp, max = 148.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filteredIcons.forEach { entry ->
                            val isSelected = selectedEmoji == entry.emoji
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) {
                                    accentColor.copy(alpha = 0.24f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(2.dp, accentColor, RoundedCornerShape(10.dp))
                                        } else {
                                            Modifier.border(
                                                1.dp,
                                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                                RoundedCornerShape(10.dp)
                                            )
                                        }
                                    )
                                    .clickable { onSelectIcon(entry) }
                                    .testTag("icon_option_${entry.label}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = entry.vector,
                                        contentDescription = entry.label,
                                        tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${entry.emoji} ${entry.label}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Structured Top Month Header + 7-Day Horizontal Week Strip.
 * Matches screenshots 3, 4, 6, 8:
 * - "novembro " (white/onBackground bold) + "2026" (accentColor bold)
 * - Right side: Calendar modal icon button + Filter/Tools toggle icon + Appearance layout pill
 * - 7-day horizontal strip (seg..dom) with circular selected pill and conditional colored dots in Completo mode.
 */
@Composable
fun StructuredAgendaHeader(
    selectedEpochDay: Long,
    accentColor: Color,
    layoutMode: String,
    tasksByDay: Map<Long, List<TaskItem>>,
    activeFiltersCount: Int,
    inboxCount: Int,
    dueBillsCount: Int,
    onSelectEpochDay: (Long) -> Unit,
    onOpenDatePickerModal: () -> Unit,
    onToggleFiltersPanel: () -> Unit,
    onOpenAppearanceSheet: () -> Unit,
    onSwitchToInbox: () -> Unit
) {
    val selectedDate = remember(selectedEpochDay) { LocalDate.ofEpochDay(selectedEpochDay) }
    val todayEpoch = remember { DateCalendarUtils.todayEpochDay() }
    val monthName = remember(selectedDate) {
        selectedDate.month.getDisplayName(TextStyle.FULL, PtBrLocale).lowercase(PtBrLocale)
    }
    val yearText = remember(selectedDate) { selectedDate.year.toString() }

    // Compute the 7 days of the week (Monday..Sunday) containing selectedDate
    val weekDates = remember(selectedDate) {
        val monday = selectedDate.with(DayOfWeek.MONDAY)
        (0L..6L).map { monday.plusDays(it) }
    }

    val dayLabels = listOf("seg", "ter", "qua", "qui", "sex", "sáb", "dom")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("structured_agenda_header"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Row: "novembro 2026" + action icons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.ExtraBold
                        )
                    ) {
                        append("$monthName ")
                    }
                    withStyle(
                        SpanStyle(
                            color = accentColor,
                            fontWeight = FontWeight.ExtraBold
                        )
                    ) {
                        append(yearText)
                    }
                },
                fontSize = 26.sp,
                modifier = Modifier
                    .clickable { onOpenDatePickerModal() }
                    .testTag("structured_month_year_title")
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Quick Inbox pill if there are inbox items
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onSwitchToInbox() }
                        .testTag("structured_inbox_shortcut_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = "Inbox",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Inbox ($inboxCount)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Layout mode shortcut button (Completo / Simplificado / Mínimo)
                IconButton(
                    onClick = onOpenAppearanceSheet,
                    modifier = Modifier.testTag("structured_appearance_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Aparência e Layout da Agenda",
                        tint = accentColor
                    )
                }

                // Expandable Filters & Tools toggle button
                BadgedBox(
                    badge = {
                        if (activeFiltersCount > 0 || dueBillsCount > 0) {
                            Badge(containerColor = accentColor) {
                                Text("${activeFiltersCount + dueBillsCount}")
                            }
                        }
                    }
                ) {
                    IconButton(
                        onClick = onToggleFiltersPanel,
                        modifier = Modifier.testTag("structured_toggle_filters_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Filtros e Opções da Agenda",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Calendar Date Picker Modal button (matching Structured top-right icon)
                IconButton(
                    onClick = onOpenDatePickerModal,
                    modifier = Modifier.testTag("structured_open_calendar_modal_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.EditCalendar,
                        contentDescription = "Selecionar data no calendário",
                        tint = accentColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // 7-Day Week Strip (seg..dom) with swipe/prev-next arrows
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            weekDates.forEachIndexed { idx, date ->
                val epoch = date.toEpochDay()
                val isSelected = epoch == selectedEpochDay
                val isToday = epoch == todayEpoch
                val isWeekend = idx >= 5
                val dayTasks = tasksByDay[epoch].orEmpty()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectEpochDay(epoch) }
                        .padding(vertical = 4.dp)
                        .testTag("week_strip_day_${date.dayOfMonth}")
                ) {
                    Text(
                        text = dayLabels[idx],
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
                        },
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isSelected -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.88f)
                                    else -> Color.Transparent
                                }
                            )
                            .then(
                                if (isToday && !isSelected) {
                                    Modifier.border(1.5.dp, accentColor, CircleShape)
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.background
                                isToday || isWeekend -> accentColor
                                else -> MaterialTheme.colorScheme.onBackground
                            }
                        )
                    }

                    // In Completo layout mode, show overlapping task indicator dots below each day
                    if (layoutMode == AgendaLayoutMode.COMPLETO) {
                        Spacer(modifier = Modifier.height(4.dp))
                        if (dayTasks.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy((-3).dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.height(10.dp)
                            ) {
                                dayTasks.take(4).forEach { t ->
                                    val dotColor = if (t.taskColorHex != 0L) Color(t.taskColorHex) else accentColor
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                            .border(1.dp, MaterialTheme.colorScheme.background, CircleShape)
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Structured Timeline Task Row (inside the rounded timeline sheet).
 * Matches screenshots 3, 4, 6, 8:
 * - Left time column ("06:30") shown in COMPLETO & SIMPLIFICADO, hidden in MINIMO.
 * - Vertical connecting line (solid above & below).
 * - Node shape:
 *     - Short tasks (<= 30m): 54.dp Circle
 *     - Long tasks (> 30m): Tall vertical rounded pill proportional to duration (e.g., 108.dp - 170.dp)
 *       with start time at top, end time at bottom, and icon centered.
 * - Center column:
 *     - Time range ("06:45 - 08:15 (1h 30m)" or "06:30") + repeat icon (hidden in MINIMO)
 *     - Task Title (17.sp bold, strikethrough when completed)
 *     - Metadata row: subtask counter ("☑️ 0/5") + notes icon ("☰") + family badge (shown in COMPLETO)
 * - Right side:
 *     - 26.dp circular checkbox with 2.dp border in taskColor/accentColor.
 */
@Composable
fun StructuredTimelineTaskRow(
    task: TaskItem,
    startMinutesOfDay: Int,
    endMinutesOfDay: Int,
    accentColor: Color,
    layoutMode: String,
    isFirstNode: Boolean,
    isLastNode: Boolean,
    nextNodeIsGap: Boolean,
    onTaskClick: (TaskItem) -> Unit,
    onToggleTask: (TaskItem) -> Unit,
    onDeleteOrRescheduleTask: ((TaskItem) -> Unit)? = null,
    onSwipeEditOrReschedule: ((TaskItem) -> Unit)? = onDeleteOrRescheduleTask,
    onSwipeDelete: ((TaskItem) -> Unit)? = null
) {
    val taskColor = remember(task.taskColorHex, accentColor) {
        if (task.taskColorHex != 0L) Color(task.taskColorHex) else accentColor
    }
    val durationMinutes = (endMinutesOfDay - startMinutesOfDay).coerceAtLeast(15)
    val isLongTask = durationMinutes > 30 && layoutMode != AgendaLayoutMode.MINIMO
    val nodeHeight: Dp = remember(durationMinutes, isLongTask) {
        if (!isLongTask) {
            54.dp
        } else {
            (72 + (durationMinutes - 30).coerceIn(15, 180) * 0.55f).dp
        }
    }

    val startHHmm = remember(startMinutesOfDay) {
        DateCalendarUtils.formatTime(startMinutesOfDay / 60, startMinutesOfDay % 60)
    }
    val endHHmm = remember(endMinutesOfDay) {
        DateCalendarUtils.formatTime(endMinutesOfDay / 60, endMinutesOfDay % 60)
    }

    val completedSubtasks = remember(task.subtasks) { task.subtasks.count { it.done } }
    val totalSubtasks = task.subtasks.size
    val iconVector = remember(task) { resolveStructuredTaskIcon(task) }

    var dragOffsetX by remember(task.id) { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(task.id, task.completed) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        when {
                            dragOffsetX > 110f -> {
                                dragOffsetX = 0f
                                onToggleTask(task)
                            }
                            dragOffsetX < -175f && onSwipeDelete != null -> {
                                dragOffsetX = 0f
                                onSwipeDelete(task)
                            }
                            dragOffsetX < -110f -> {
                                dragOffsetX = 0f
                                when {
                                    onSwipeEditOrReschedule != null -> onSwipeEditOrReschedule(task)
                                    onDeleteOrRescheduleTask != null -> onDeleteOrRescheduleTask(task)
                                    onSwipeDelete != null -> onSwipeDelete(task)
                                    else -> onTaskClick(task)
                                }
                            }
                            else -> {
                                dragOffsetX = 0f
                            }
                        }
                    },
                    onDragCancel = { dragOffsetX = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        dragOffsetX = (dragOffsetX + dragAmount).coerceIn(-220f, 220f)
                    }
                )
            }
    ) {
        if (kotlin.math.abs(dragOffsetX) > 20f) {
            val isSwipingRight = dragOffsetX > 0f
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSwipingRight) Color(0xFF10B981).copy(alpha = 0.22f)
                        else Color(0xFFF59E0B).copy(alpha = 0.22f)
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isSwipingRight) Arrangement.Start else Arrangement.End
            ) {
                Text(
                    text = if (isSwipingRight) "✓ Concluir" else "🗓️ Reagendar / Excluir",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isSwipingRight) Color(0xFF10B981) else Color(0xFFF59E0B),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Row(
            modifier = Modifier
                .offset { IntOffset(dragOffsetX.roundToInt(), 0) }
                .fillMaxWidth()
                .clickable { onTaskClick(task) }
                .padding(horizontal = 14.dp)
                .testTag("structured_task_row_${task.id}"),
            verticalAlignment = Alignment.CenterVertically
        ) {
        // Optional left time label in SIMPLIFICADO/COMPLETO for short tasks or preview alignment
        // Note: In Structured screenshots 4, 6, 8, tall pills show start/end time right next to the vertical line,
        // while the appearance preview shows the time column on the left. We show a clean time column when not a tall pill or in Completo.
        if (layoutMode != AgendaLayoutMode.MINIMO) {
            Column(
                modifier = Modifier.width(44.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = startHHmm,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                    fontSize = 11.sp
                )
                if (isLongTask) {
                    Spacer(modifier = Modifier.height(nodeHeight - 34.dp))
                    Text(
                        text = endHHmm,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
        }

        // Timeline Node + Vertical Connecting Line
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(nodeHeight + 24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Vertical connecting lines above and below the node
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2f
                val strokeWidth = 3.dp.toPx()
                if (!isFirstNode) {
                    drawLine(
                        color = taskColor.copy(alpha = 0.75f),
                        start = Offset(centerX, 0f),
                        end = Offset(centerX, size.height / 2f),
                        strokeWidth = strokeWidth
                    )
                }
                if (!isLastNode) {
                    val pathEffect = if (nextNodeIsGap) {
                        PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    } else {
                        null
                    }
                    drawLine(
                        color = taskColor.copy(alpha = 0.75f),
                        start = Offset(centerX, size.height / 2f),
                        end = Offset(centerX, size.height),
                        strokeWidth = strokeWidth,
                        pathEffect = pathEffect
                    )
                }
            }

            // Circle (<= 30m) or Tall Vertical Pill (> 30m)
            Box(
                modifier = Modifier
                    .width(54.dp)
                    .height(nodeHeight)
                    .clip(RoundedCornerShape(27.dp))
                    .background(
                        if (task.completed) taskColor.copy(alpha = 0.45f) else taskColor.copy(alpha = 0.85f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = task.title,
                    tint = Color(0xFF1A1B1E),
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Center Task Info
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            if (layoutMode != AgendaLayoutMode.MINIMO) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    val actualDurationMin = task.durationMinutes.coerceAtLeast(durationMinutes)
                    val actualDurationLabel = formatTaskDurationHumanPt(actualDurationMin)
                    val timeHeaderText = "$startHHmm - $endHHmm • $actualDurationLabel ($actualDurationMin min)"
                    Text(
                        text = timeHeaderText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Recorrente",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                ),
                fontWeight = FontWeight.Bold,
                color = if (task.completed) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Location & Date Period Badges (shown whenever present; clicking location opens Google Maps)
            if (task.location.isNotBlank() || task.hasDateRange) {
                val context = LocalContext.current
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    if (task.location.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = taskColor.copy(alpha = 0.14f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { openLocationInGoogleMaps(context, task.location) }
                                .testTag("open_task_location_maps_${task.id}")
                        ) {
                            Text(
                                text = "📍 ${task.location} (Maps ↗)",
                                style = MaterialTheme.typography.labelSmall,
                                color = taskColor,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (task.hasDateRange) {
                        Text(
                            text = "🗓️ ${DateCalendarUtils.formatEpochDayPt(task.dateEpochDay)} até ${DateCalendarUtils.formatEpochDayPt(task.endDateEpochDay)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = taskColor,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Extra metadata shown in COMPLETO mode (or subtasks count in SIMPLIFICADO)
            if (layoutMode == AgendaLayoutMode.COMPLETO) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 3.dp)
                ) {
                    if (totalSubtasks > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckBox,
                                contentDescription = "Subtarefas",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "$completedSubtasks/$totalSubtasks",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (task.notes.isNotBlank()) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Notes,
                            contentDescription = "Possui notas",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (task.privacyScope != PrivacyScope.PERSONAL) {
                        Text(
                            text = "Família",
                            style = MaterialTheme.typography.labelSmall,
                            color = taskColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (task.isEssential) {
                        Text(
                            text = "★ Essencial",
                            style = MaterialTheme.typography.labelSmall,
                            color = taskColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (layoutMode == AgendaLayoutMode.SIMPLIFICADO && totalSubtasks > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckBox,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "$completedSubtasks/$totalSubtasks",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Right Circular Checkbox (matching Structured's clean circular ring / checkmark)
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (task.completed) taskColor else Color.Transparent)
                .border(2.dp, taskColor, CircleShape)
                .clickable { onToggleTask(task) }
                .testTag("structured_task_checkbox_${task.id}"),
            contentAlignment = Alignment.Center
        ) {
            if (task.completed) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Concluída",
                    tint = Color(0xFF1A1B1E),
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
    }
}

/**
 * Structured Free Gap Row along the vertical connecting line.
 * Compacted into a discreet dashed line and a small "+ Agendar" button so the timeline stays clean.
 */
@Composable
fun StructuredFreeGapRow(
    startMinutesOfDay: Int,
    endMinutesOfDay: Int,
    gapMinutes: Int,
    accentColor: Color,
    layoutMode: String,
    onAddInGap: () -> Unit
) {
    if (layoutMode == AgendaLayoutMode.MINIMO) return

    val durationLabel = remember(gapMinutes) {
        val h = gapMinutes / 60
        val m = gapMinutes % 60
        when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    }
    val startHHmm = remember(startMinutesOfDay) {
        DateCalendarUtils.formatTime(startMinutesOfDay / 60, startMinutesOfDay % 60)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAddInGap() }
            .padding(horizontal = 14.dp)
            .testTag("structured_free_gap_$startMinutesOfDay"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left time offset column to align with task nodes
        Column(
            modifier = Modifier.width(44.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = startHHmm,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = JetBrainsMonoFontFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                fontSize = 10.sp,
                maxLines = 1,
                softWrap = false
            )
        }
        Spacer(modifier = Modifier.width(10.dp))

        // Discreet dashed vertical line segment
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(34.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2f
                drawLine(
                    color = accentColor.copy(alpha = 0.45f),
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, size.height),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$durationLabel livre",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                maxLines = 1,
                softWrap = false
            )
            Surface(
                shape = com.example.ui.theme.omniOrganicButtonShape(),
                color = accentColor.copy(alpha = 0.14f),
                border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "+ Agendar",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

/**
 * Structured Task Detail Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StructuredTaskDetailBottomSheet(
    task: TaskItem,
    accentColor: Color,
    onDismiss: () -> Unit,
    onToggleTask: (TaskItem) -> Unit,
    onToggleSubtask: (TaskItem, String) -> Unit,
    onToggleEssential: (TaskItem) -> Unit,
    onEditTask: (TaskItem) -> Unit = {},
    onDeleteTask: (TaskItem) -> Unit
) {
    val context = LocalContext.current
    val taskColor = if (task.taskColorHex != 0L) Color(task.taskColorHex) else accentColor
    val startTotalMinutes = task.alarmHour.toInt() * 60 + task.alarmMinute.toInt()
    val safeDuration = task.durationMinutes.coerceIn(5, 720)
    val endTotalMinutes = (startTotalMinutes + safeDuration) % (24 * 60)
    val startHHmm = DateCalendarUtils.formatTime(task.alarmHour.toInt(), task.alarmMinute.toInt())
    val endHHmm = DateCalendarUtils.formatTime(endTotalMinutes / 60, endTotalMinutes % 60)
    val humanDuration = formatTaskDurationHumanPt(safeDuration)
    val iconVector = remember(task) { resolveStructuredTaskIcon(task) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF18191C),
        contentColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(44.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(taskColor)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .testTag("structured_task_detail_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with circular icon, subtitle/time, and title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(taskColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = Color(0xFF18191C),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val subtitleInfo = if (task.isInbox) {
                        "📥 Inbox (Sem horário definido) • ${task.category.ifBlank { "Rotina" }}"
                    } else {
                        "$startHHmm - $endHHmm • $humanDuration ($safeDuration min) • ${task.category.ifBlank { "Rotina" }}"
                    }
                    Text(
                        text = subtitleInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                IconButton(onClick = { onToggleEssential(task) }) {
                    Icon(
                        imageVector = if (task.isEssential) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Essencial",
                        tint = if (task.isEssential) taskColor else Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.14f))

            if (task.location.isNotBlank() || task.hasDateRange) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (task.location.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF25262B),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { openLocationInGoogleMaps(context, task.location) }
                                .testTag("sheet_open_location_maps_btn")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📍 Local: ${task.location}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.92f),
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Abrir no Maps ↗",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = taskColor
                                )
                            }
                        }
                    }
                    if (task.hasDateRange) {
                        Text(
                            text = "🗓️ Período: ${DateCalendarUtils.formatEpochDayPt(task.dateEpochDay)} até ${DateCalendarUtils.formatEpochDayPt(task.endDateEpochDay)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = taskColor
                        )
                    }
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
            }

            // Notes / Description
            if (task.notes.isNotBlank()) {
                Text(
                    text = task.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 20.sp
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
            }

            // Subtasks Checklist (matching screenshot 3 square coral checkboxes + dividers)
            if (task.subtasks.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    task.subtasks.forEach { sub ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleSubtask(task, sub.id) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (sub.done) taskColor else Color.Transparent)
                                    .border(2.dp, taskColor, RoundedCornerShape(5.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (sub.done) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF18191C),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = sub.title,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    textDecoration = if (sub.done) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                fontWeight = FontWeight.SemiBold,
                                color = if (sub.done) Color.White.copy(alpha = 0.5f) else Color.White
                            )
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                    }
                }
            }

            // Bottom Actions: Concluir, Editar & Excluir
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF25262B),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            onToggleTask(task)
                            onDismiss()
                        }
                        .testTag("structured_sheet_toggle_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = taskColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (task.completed) "Reabrir" else "Concluir",
                            fontWeight = FontWeight.Bold,
                            color = taskColor
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF25262B),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            onDismiss()
                            onEditTask(task)
                        }
                        .testTag("structured_sheet_edit_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = taskColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Editar",
                            fontWeight = FontWeight.Bold,
                            color = taskColor
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF25262B),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            onDismiss()
                            onDeleteTask(task)
                        }
                        .testTag("structured_sheet_delete_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = taskColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Excluir",
                            fontWeight = FontWeight.Bold,
                            color = taskColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * Structured "Selecionar data" Calendar Modal Dialog with colored member/task dots below each day number.
 */
@Composable
fun StructuredDatePickerDialog(
    selectedEpochDay: Long,
    accentColor: Color,
    tasks: List<TaskItem> = emptyList(),
    onDismiss: () -> Unit,
    onConfirmEpochDay: (Long) -> Unit
) {
    var tempSelectedDate by remember(selectedEpochDay) {
        mutableStateOf(LocalDate.ofEpochDay(selectedEpochDay))
    }
    var currentMonth by remember(selectedEpochDay) {
        mutableStateOf(YearMonth.from(LocalDate.ofEpochDay(selectedEpochDay)))
    }
    val today = remember { LocalDate.ofEpochDay(DateCalendarUtils.todayEpochDay()) }

    val formattedSelectedHeader = remember(tempSelectedDate) {
        val dayOfWeekShort = tempSelectedDate.dayOfWeek.getDisplayName(TextStyle.SHORT, PtBrLocale)
            .lowercase(PtBrLocale)
            .removeSuffix(".")
        val monthShort = tempSelectedDate.month.getDisplayName(TextStyle.SHORT, PtBrLocale)
            .lowercase(PtBrLocale)
            .removeSuffix(".")
        "$dayOfWeekShort., ${tempSelectedDate.dayOfMonth} de $monthShort."
    }

    val monthYearLabel = remember(currentMonth) {
        val m = currentMonth.month.getDisplayName(TextStyle.FULL, PtBrLocale).lowercase(PtBrLocale)
        "$m de ${currentMonth.year}"
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF242222),
            contentColor = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("structured_date_picker_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Selecionar data",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.75f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedSelectedHeader,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

                // Month switcher row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = monthYearLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Row {
                        IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Mês anterior",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Próximo mês",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Weekday headers: D S T Q Q S S
                val weekHeaders = listOf("D", "S", "T", "Q", "Q", "S", "S")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weekHeaders.forEach { h ->
                        Box(
                            modifier = Modifier.size(38.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = h,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Calendar Days Grid with colored member/task dots below each day number
                val firstDayOfMonth = currentMonth.atDay(1)
                val leadingEmpty = firstDayOfMonth.dayOfWeek.value % 7
                val daysInMonth = currentMonth.lengthOfMonth()
                val totalSlots = ((leadingEmpty + daysInMonth + 6) / 7) * 7

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (week in 0 until (totalSlots / 7)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (dow in 0..6) {
                                val index = week * 7 + dow
                                val dayNum = index - leadingEmpty + 1
                                if (dayNum in 1..daysInMonth) {
                                    val cellDate = currentMonth.atDay(dayNum)
                                    val cellEpoch = cellDate.toEpochDay()
                                    val isSelected = cellDate == tempSelectedDate
                                    val isToday = cellDate == today
                                    val dayTaskColors = remember(tasks, cellEpoch, accentColor) {
                                        tasks.asSequence()
                                            .filter { !it.isInbox && it.occursOnEpochDay(cellEpoch) }
                                            .map { t ->
                                                when {
                                                    t.taskColorHex != 0L -> Color(t.taskColorHex)
                                                    t.assignedMemberColorHex != 0L -> Color(t.assignedMemberColorHex)
                                                    else -> accentColor
                                                }
                                            }
                                            .distinct()
                                            .take(4)
                                            .toList()
                                    }

                                    Column(
                                        modifier = Modifier
                                            .width(38.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { tempSelectedDate = cellDate }
                                            .padding(vertical = 2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) accentColor else Color.Transparent)
                                                .then(
                                                    if (isToday && !isSelected) {
                                                        Modifier.border(1.5.dp, accentColor, CircleShape)
                                                    } else {
                                                        Modifier
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = dayNum.toString(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                color = when {
                                                    isSelected -> Color(0xFF18191C)
                                                    isToday -> accentColor
                                                    else -> Color.White
                                                }
                                            )
                                        }
                                        if (dayTaskColors.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                dayTaskColors.forEach { dotCol ->
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(dotCol)
                                                    )
                                                }
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.height(7.dp))
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(38.dp))
                                }
                            }
                        }
                    }
                }

                // Bottom buttons: Cancelar | OK
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Cancelar",
                            color = accentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            onConfirmEpochDay(tempSelectedDate.toEpochDay())
                            onDismiss()
                        }
                    ) {
                        Text(
                            text = "OK",
                            color = accentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Structured "Aparência" Settings Card (matching Screenshots 1, 2, 7:
 * Screenshot_20261121_111328, 111330, 111335).
 * Includes:
 * 1. Live Interactive Preview Card that updates in real time as the user toggles
 *    between "Completo", "Simplificado", and "Mínimo" and picks a "COR DO APP".
 * 2. "COR DO APP" horizontal circular swatches + "Editar paleta" action.
 * 3. "LAYOUT" 3-option segmented selector (Completo | Simplificado | Mínimo) + description.
 */
@Composable
fun StructuredAppearanceCard(
    config: AppCustomizationConfig,
    customizationRepo: CustomizationRepository,
    onOpenFullPalette: () -> Unit = {}
) {
    var selectedModuleId by remember { mutableStateOf(AppModuleId.AGENDA) }
    val activeModuleColorHex = config.colorForModule(selectedModuleId)
    val accentColor = Color(activeModuleColorHex)
    val agendaAccentColor = Color(config.agendaColorHex)
    val layoutMode = config.agendaLayoutMode
    val organicStyle = config.organicVisualStyle

    val previewCornerRadius = when (organicStyle) {
        OrganicVisualStyle.ORGANICO_FLUIDO -> 24.dp
        OrganicVisualStyle.TRANSLUCIDO_GLASS -> 20.dp
        OrganicVisualStyle.MINIMALISTA_ACOLHEDOR -> 10.dp
        else -> 24.dp
    }
    val previewButtonShape = com.example.ui.theme.omniOrganicButtonShape(organicStyle)
    val previewCardColor = when (organicStyle) {
        OrganicVisualStyle.TRANSLUCIDO_GLASS -> Color(0xFF282A2E).copy(alpha = 0.65f)
        OrganicVisualStyle.MINIMALISTA_ACOLHEDOR -> Color(0xFF1E2024)
        else -> Color(0xFF282A2E)
    }
    val previewBorderStroke = when (organicStyle) {
        OrganicVisualStyle.TRANSLUCIDO_GLASS -> androidx.compose.foundation.BorderStroke(1.dp, agendaAccentColor.copy(alpha = 0.55f))
        OrganicVisualStyle.MINIMALISTA_ACOLHEDOR -> androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
        else -> androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (organicStyle == OrganicVisualStyle.TRANSLUCIDO_GLASS) accentColor.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(previewCornerRadius)
            )
            .testTag("structured_appearance_settings_card"),
        shape = RoundedCornerShape(previewCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = if (organicStyle == OrganicVisualStyle.TRANSLUCIDO_GLASS) Color(0xFF141518).copy(alpha = 0.82f) else Color(0xFF141518)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (organicStyle == OrganicVisualStyle.MINIMALISTA_ACOLHEDOR) 0.dp else 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Aparência & Cores dos Módulos",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Personalize cada módulo separadamente, escolha temas sugeridos e o estilo orgânico",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.68f)
                    )
                }
            }

            // 1. LIVE INTERACTIVE PREVIEW CARD (Reflects OrganicVisualStyle + LayoutMode + 5 Module Colors)
            Surface(
                shape = RoundedCornerShape(previewCornerRadius),
                color = previewCardColor,
                border = previewBorderStroke,
                shadowElevation = if (organicStyle == OrganicVisualStyle.MINIMALISTA_ACOLHEDOR) 0.dp else 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("structured_appearance_live_preview")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top 7-day preview header inside the mockup
                    Surface(
                        color = if (organicStyle == OrganicVisualStyle.TRANSLUCIDO_GLASS) Color(0xFF3A3C42).copy(alpha = 0.65f) else Color(0xFF3A3C42),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (organicStyle) {
                                        OrganicVisualStyle.TRANSLUCIDO_GLASS -> "Prévia: Translúcido (Glass 65% + Borda Luminosa)"
                                        OrganicVisualStyle.MINIMALISTA_ACOLHEDOR -> "Prévia: Suave Minimal (10dp Plano + Borda 1dp)"
                                        else -> "Prévia: Pílula & Fluido (24dp + Botões Pílula)"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = agendaAccentColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                for (day in 1..7) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(22.dp)
                                                .height(10.dp)
                                                .clip(previewButtonShape)
                                                .background(Color.White.copy(alpha = 0.35f))
                                        )
                                        Text(
                                            text = day.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                        if (layoutMode == AgendaLayoutMode.COMPLETO) {
                                            Row(horizontalArrangement = Arrangement.spacedBy((-3).dp)) {
                                                listOf(
                                                    Color(config.agendaColorHex),
                                                    Color(config.hubColorHex),
                                                    Color(config.healthColorHex),
                                                    Color(config.financeColorHex)
                                                ).forEach { dotCol ->
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(dotCol)
                                                            .border(1.dp, Color(0xFF3A3C42), CircleShape)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Preview Timeline Body
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        PreviewTimelineItem(
                            timeText = "13:00",
                            icon = Icons.Default.LaptopMac,
                            accentColor = agendaAccentColor,
                            showTime = layoutMode != AgendaLayoutMode.MINIMO,
                            connectBelow = true
                        )
                        PreviewTimelineItem(
                            timeText = "13:15",
                            icon = Icons.Default.Restaurant,
                            accentColor = agendaAccentColor,
                            showTime = layoutMode != AgendaLayoutMode.MINIMO,
                            connectBelow = true
                        )

                        if (layoutMode != AgendaLayoutMode.MINIMO) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "13:30",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.65f),
                                    modifier = Modifier.width(38.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .width(36.dp)
                                        .height(28.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .fillMaxHeight()
                                            .background(agendaAccentColor.copy(alpha = 0.55f))
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Surface(
                                    shape = previewButtonShape,
                                    color = agendaAccentColor.copy(alpha = 0.18f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, agendaAccentColor.copy(alpha = 0.45f))
                                ) {
                                    Text(
                                        text = "+ Agendar",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = agendaAccentColor,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        PreviewTimelineItem(
                            timeText = "14:00",
                            icon = Icons.Default.Call,
                            accentColor = agendaAccentColor,
                            showTime = layoutMode != AgendaLayoutMode.MINIMO,
                            connectBelow = layoutMode == AgendaLayoutMode.MINIMO
                        )

                        if (layoutMode == AgendaLayoutMode.MINIMO) {
                            PreviewTimelineItem(
                                timeText = "15:00",
                                icon = Icons.Default.FormatListBulleted,
                                accentColor = agendaAccentColor,
                                showTime = false,
                                connectBelow = false
                            )
                        }
                    }

                    // Mini Bar Showing the 5 Module Colors Live
                    Surface(
                        color = Color(0xFF1F2126),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CustomizationCatalog.moduleSpecs.forEach { spec ->
                                val modColor = Color(config.colorForModule(spec.moduleId))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .clip(CircleShape)
                                            .background(modColor)
                                    )
                                    Text(
                                        text = spec.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. SUGESTÕES DE TEMA PRONTOS (Aplicam paleta harmônica para o app e os 5 módulos)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SUGESTÕES DE TEMA (PALETAS HARMÔNICAS)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.65f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CustomizationCatalog.presets.forEach { preset ->
                        val isPresetSelected = !config.useCustomColors && config.presetId == preset.id
                        Surface(
                            shape = RoundedCornerShape(previewCornerRadius),
                            color = if (isPresetSelected) Color(preset.primaryHex).copy(alpha = 0.25f) else Color(0xFF282A2E),
                            modifier = Modifier
                                .clip(RoundedCornerShape(previewCornerRadius))
                                .border(
                                    width = if (isPresetSelected) 2.dp else 1.dp,
                                    color = if (isPresetSelected) Color(preset.agendaColorHex) else Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(previewCornerRadius)
                                )
                                .clickable {
                                    customizationRepo.applyThemePreset(preset)
                                }
                                .testTag("theme_suggestion_${preset.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    listOf(
                                        preset.hubColorHex,
                                        preset.agendaColorHex,
                                        preset.healthColorHex,
                                        preset.diaryColorHex,
                                        preset.financeColorHex
                                    ).forEach { dotHex ->
                                        Box(
                                            modifier = Modifier
                                                .size(13.dp)
                                                .clip(CircleShape)
                                                .background(Color(dotHex))
                                                .border(1.dp, Color.Black.copy(alpha = 0.3f), CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. CORES INDEPENDENTES DOS 5 MÓDULOS (Hub, Agenda, Saúde, Diário, Finanças)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "COR POR MÓDULO (5 MÓDULOS)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = previewButtonShape,
                        color = accentColor.copy(alpha = 0.16f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .clip(previewButtonShape)
                            .clickable { onOpenFullPalette() }
                            .testTag("open_fonts_and_sounds_btn")
                    ) {
                        Text(
                            text = "Fontes & Sons",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                // Seletor em pílulas dos 5 módulos (scroll horizontal garantido)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CustomizationCatalog.moduleSpecs.forEach { spec ->
                        val isSelectedMod = selectedModuleId == spec.moduleId
                        val modColor = Color(config.colorForModule(spec.moduleId))
                        Surface(
                            shape = previewButtonShape,
                            color = if (isSelectedMod) modColor.copy(alpha = 0.24f) else Color(0xFF282A2E),
                            modifier = Modifier
                                .clip(previewButtonShape)
                                .border(
                                    width = if (isSelectedMod) 2.dp else 1.dp,
                                    color = if (isSelectedMod) modColor else Color.White.copy(alpha = 0.1f),
                                    shape = previewButtonShape
                                )
                                .clickable { selectedModuleId = spec.moduleId }
                                .testTag("module_color_tab_${spec.moduleId}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(modColor)
                                )
                                Text(
                                    text = spec.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelectedMod) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelectedMod) Color.White else Color.White.copy(alpha = 0.75f),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                // Barra de Swatches Circulares para o Módulo Selecionado
                Surface(
                    shape = RoundedCornerShape(previewCornerRadius),
                    color = Color(0xFF282A2E),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CustomizationCatalog.structuredAppColors.forEachIndexed { idx, (_, colorHex) ->
                            val swatchColor = Color(colorHex)
                            val isSelected = activeModuleColorHex == colorHex
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                                    .then(
                                        if (isSelected) {
                                            Modifier
                                                .border(2.5.dp, Color(0xFF141518), CircleShape)
                                                .border(4.dp, Color.White, CircleShape)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable {
                                        customizationRepo.updateModuleColor(selectedModuleId, colorHex)
                                    }
                                    .testTag("structured_color_swatch_$idx")
                            )
                        }
                    }
                }

                val selectedModuleSpec = CustomizationCatalog.moduleSpecs.firstOrNull { it.moduleId == selectedModuleId }
                Text(
                    text = "Personalizando: ${selectedModuleSpec?.label ?: "Agenda"} — ${selectedModuleSpec?.description ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.68f)
                )
            }

            // 4. ESTILO VISUAL ORGÂNICO (Altera imediatamente a prévia e todos os cards/botões do app)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ESTILO VISUAL ORGÂNICO",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.65f)
                )

                Surface(
                    shape = RoundedCornerShape(previewCornerRadius),
                    color = Color(0xFF282A2E),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val styles = listOf(
                            OrganicVisualStyle.ORGANICO_FLUIDO to "Pílula & Fluido",
                            OrganicVisualStyle.TRANSLUCIDO_GLASS to "Translúcido",
                            OrganicVisualStyle.MINIMALISTA_ACOLHEDOR to "Suave Minimal"
                        )
                        styles.forEach { (styleKey, label) ->
                            val selected = organicStyle == styleKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(previewButtonShape)
                                    .background(if (selected) accentColor else Color.Transparent)
                                    .clickable {
                                        customizationRepo.updateOrganicVisualStyle(styleKey)
                                    }
                                    .padding(horizontal = 4.dp, vertical = 11.dp)
                                    .testTag("organic_visual_style_$styleKey"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color(0xFF141518) else Color.White.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 5. LAYOUT DA AGENDA Section (Completo | Simplificado | Mínimo)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "LAYOUT DA AGENDA",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.65f)
                )

                Surface(
                    shape = RoundedCornerShape(previewCornerRadius),
                    color = Color(0xFF282A2E),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val options = listOf(
                            AgendaLayoutMode.COMPLETO to "Completo",
                            AgendaLayoutMode.SIMPLIFICADO to "Simplificado",
                            AgendaLayoutMode.MINIMO to "Mínimo"
                        )
                        options.forEach { (modeKey, label) ->
                            val selected = layoutMode == modeKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(previewButtonShape)
                                    .background(if (selected) agendaAccentColor else Color.Transparent)
                                    .clickable {
                                        customizationRepo.updateAgendaLayoutMode(modeKey)
                                    }
                                    .padding(horizontal = 4.dp, vertical = 12.dp)
                                    .testTag("structured_layout_mode_$modeKey"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color(0xFF141518) else agendaAccentColor,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Text(
                    text = buildAnnotatedString {
                        append("Os layouts ")
                        withStyle(SpanStyle(color = agendaAccentColor, fontWeight = FontWeight.SemiBold)) {
                            append("Simplificado")
                        }
                        append(" e ")
                        withStyle(SpanStyle(color = agendaAccentColor, fontWeight = FontWeight.SemiBold)) {
                            append("Mínimo")
                        }
                        append(" esconderão certos elementos para a interface causar menos distrações.")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f)
                )
            }
        }
    }
}

@Composable
private fun PreviewTimelineItem(
    timeText: String,
    icon: ImageVector,
    accentColor: Color,
    showTime: Boolean,
    connectBelow: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showTime) {
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.65f),
                modifier = Modifier.width(38.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            if (connectBelow) {
                Box(
                    modifier = Modifier
                        .width(2.5.dp)
                        .height(14.dp)
                        .background(accentColor)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.25f))
            )
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.35f))
            )
        }

        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(accentColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFF282A2E),
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
