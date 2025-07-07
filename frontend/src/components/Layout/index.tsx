import { useState } from 'react';
import { useNavigate, Outlet } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import {
  AppBar,
  Box,
  CssBaseline,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Typography,
  Menu,
  MenuItem,
  Collapse,
} from '@mui/material';
import {
  Menu as MenuIcon,
  Dashboard,
  People,
  AttachMoney,
  CardGiftcard,
  Assessment,
  AccountCircle,
  ExpandLess,
  ExpandMore,
  Settings,
  Business,
  Work,
  AccountTree,
  Receipt,
  CloudUpload,
} from '@mui/icons-material';

const drawerWidth = 240;

export function Layout() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [cadastroOpen, setCadastroOpen] = useState(false);

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen);
  };

  const handleMenu = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleLogout = () => {
    handleClose();
    logout();
  };

  const handleCadastroClick = () => {
    setCadastroOpen(!cadastroOpen);
  };

  const menuItems = [
    { text: 'Dashboard', icon: <Dashboard />, path: '/dashboard' },
    { text: 'Funcionários', icon: <People />, path: '/funcionarios' },
    { text: 'Folha de Pagamento', icon: <AttachMoney />, path: '/folha-pagamento' },
    { text: 'Benefícios', icon: <CardGiftcard />, path: '/beneficios' },
    { text: 'Importação', icon: <CloudUpload />, path: '/importacao' },
    { text: 'Relatórios', icon: <Assessment />, path: '/relatorios' },
  ];

  const cadastroItems = [
    { text: 'Linhas de Negócio', icon: <AccountTree />, path: '/linhas-negocio' },
    { text: 'Centros de Custo', icon: <Business />, path: '/centros-custo' },
    { text: 'Cargos', icon: <Work />, path: '/cargos' },
    { text: 'Rubricas', icon: <Receipt />, path: '/rubricas' },
  ];

  const drawer = (
    <div>
      <Toolbar>
        <Typography variant="h6" noWrap component="div">
          Sistema de Folha
        </Typography>
      </Toolbar>
      <Divider />
      <List>
        {menuItems.map((item) => (
          <ListItem key={item.text} disablePadding>
            <ListItemButton onClick={() => navigate(item.path)}>
              <ListItemIcon>{item.icon}</ListItemIcon>
              <ListItemText primary={item.text} />
            </ListItemButton>
          </ListItem>
        ))}
        <Divider />
        <ListItem disablePadding>
          <ListItemButton onClick={handleCadastroClick}>
            <ListItemIcon><Settings /></ListItemIcon>
            <ListItemText primary="Cadastros" />
            {cadastroOpen ? <ExpandLess /> : <ExpandMore />}
          </ListItemButton>
        </ListItem>
        <Collapse in={cadastroOpen} timeout="auto" unmountOnExit>
          <List component="div" disablePadding>
            {cadastroItems.map((item) => (
              <ListItem key={item.text} disablePadding>
                <ListItemButton sx={{ pl: 4 }} onClick={() => navigate(item.path)}>
                  <ListItemIcon>{item.icon}</ListItemIcon>
                  <ListItemText primary={item.text} />
                </ListItemButton>
              </ListItem>
            ))}
          </List>
        </Collapse>
      </List>
    </div>
  );

  return (
    <Box sx={{ display: 'flex' }}>
      <CssBaseline />
      <AppBar
        position="fixed"
        sx={{
          width: { sm: `calc(100% - ${drawerWidth}px)` },
          ml: { sm: `${drawerWidth}px` },
        }}
      >
        <Toolbar>
          <IconButton
            color="inherit"
            aria-label="open drawer"
            edge="start"
            onClick={handleDrawerToggle}
            sx={{ mr: 2, display: { sm: 'none' } }}
          >
            <MenuIcon />
          </IconButton>
          <Box sx={{ flexGrow: 1 }} />
          <div>
            <IconButton
              size="large"
              aria-label="account of current user"
              aria-controls="menu-appbar"
              aria-haspopup="true"
              onClick={handleMenu}
              color="inherit"
            >
              <AccountCircle />
            </IconButton>
            <Menu
              id="menu-appbar"
              anchorEl={anchorEl}
              anchorOrigin={{
                vertical: 'bottom',
                horizontal: 'right',
              }}
              keepMounted
              transformOrigin={{
                vertical: 'top',
                horizontal: 'right',
              }}
              open={Boolean(anchorEl)}
              onClose={handleClose}
            >
              <MenuItem disabled>
                <Typography variant="body2">{user?.nome}</Typography>
              </MenuItem>
              <Divider />
              <MenuItem onClick={handleLogout}>Sair</MenuItem>
            </Menu>
          </div>
        </Toolbar>
      </AppBar>
      <Box
        component="nav"
        sx={{ width: { sm: drawerWidth }, flexShrink: { sm: 0 } }}
      >
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={handleDrawerToggle}
          ModalProps={{
            keepMounted: true,
          }}
          sx={{
            display: { xs: 'block', sm: 'none' },
            '& .MuiDrawer-paper': {
              boxSizing: 'border-box',
              width: drawerWidth,
            },
          }}
        >
          {drawer}
        </Drawer>
        <Drawer
          variant="permanent"
          sx={{
            display: { xs: 'none', sm: 'block' },
            '& .MuiDrawer-paper': {
              boxSizing: 'border-box',
              width: drawerWidth,
            },
          }}
          open
        >
          {drawer}
        </Drawer>
      </Box>
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 3,
          width: { sm: `calc(100% - ${drawerWidth}px)` },
        }}
      >
        <Toolbar />
        <Outlet />
      </Box>
    </Box>
  );
} 